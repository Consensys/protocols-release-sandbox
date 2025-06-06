/*
 * Copyright contributors to Hyperledger Besu.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hyperledger.besu.cli.options;

import org.hyperledger.besu.cli.DefaultCommandValues;
import org.hyperledger.besu.ethereum.api.jsonrpc.RpcApis;
import org.hyperledger.besu.ethereum.p2p.peers.EnodeDnsConfiguration;
import org.hyperledger.besu.ethereum.permissioning.LocalPermissioningConfiguration;
import org.hyperledger.besu.ethereum.permissioning.PermissioningConfiguration;
import org.hyperledger.besu.ethereum.permissioning.PermissioningConfigurationBuilder;

import java.nio.file.Path;
import java.util.Optional;

import org.slf4j.Logger;
import picocli.CommandLine;

/** Handles configuration options for permissions in Besu. */
// TODO: implement CLIOption<PermissioningConfiguration>
public class PermissionsOptions {
  @CommandLine.Option(
      names = {"--permissions-nodes-config-file-enabled"},
      description = "Enable node level permissions (default: ${DEFAULT-VALUE})")
  private final Boolean permissionsNodesEnabled = false;

  @SuppressWarnings({"FieldCanBeFinal", "FieldMayBeFinal"}) // PicoCLI requires non-final Strings.
  @CommandLine.Option(
      names = {"--permissions-nodes-config-file"},
      description =
          "Node permissioning config TOML file (default: a file named \"permissions_config.toml\" in the Besu data folder)")
  private String nodePermissionsConfigFile = null;

  @CommandLine.Option(
      names = {"--permissions-accounts-config-file-enabled"},
      description = "Enable account level permissions (default: ${DEFAULT-VALUE})")
  private final Boolean permissionsAccountsEnabled = false;

  @SuppressWarnings({"FieldCanBeFinal", "FieldMayBeFinal"}) // PicoCLI requires non-final Strings.
  @CommandLine.Option(
      names = {"--permissions-accounts-config-file"},
      description =
          "Account permissioning config TOML file (default: a file named \"permissions_config.toml\" in the Besu data folder)")
  private String accountPermissionsConfigFile = null;

  /** Default constructor. */
  public PermissionsOptions() {}

  /**
   * Creates a PermissioningConfiguration based on the provided options.
   *
   * @param jsonRpcHttpOptions The JSON-RPC HTTP options
   * @param rpcWebsocketOptions The RPC websocket options
   * @param enodeDnsConfiguration The enode DNS configuration
   * @param dataPath The data path
   * @param logger The logger
   * @param commandLine The command line
   * @return An Optional PermissioningConfiguration instance
   * @throws Exception If an error occurs while creating the configuration
   */
  public Optional<PermissioningConfiguration> permissioningConfiguration(
      final JsonRpcHttpOptions jsonRpcHttpOptions,
      final RpcWebsocketOptions rpcWebsocketOptions,
      final EnodeDnsConfiguration enodeDnsConfiguration,
      final Path dataPath,
      final Logger logger,
      final CommandLine commandLine)
      throws Exception {
    if (!(localPermissionsEnabled())) {
      if (jsonRpcHttpOptions.getRpcHttpApis().contains(RpcApis.PERM.name())
          || rpcWebsocketOptions.getRpcWsApis().contains(RpcApis.PERM.name())) {
        logger.warn(
            "Permissions are disabled. Cannot enable PERM APIs when not using Permissions.");
      }
      return Optional.empty();
    }

    final Optional<LocalPermissioningConfiguration> localPermissioningConfigurationOptional;
    if (localPermissionsEnabled()) {
      final Optional<String> nodePermissioningConfigFile =
          Optional.ofNullable(nodePermissionsConfigFile);
      final Optional<String> accountPermissioningConfigFile =
          Optional.ofNullable(accountPermissionsConfigFile);

      final LocalPermissioningConfiguration localPermissioningConfiguration =
          PermissioningConfigurationBuilder.permissioningConfiguration(
              permissionsNodesEnabled,
              enodeDnsConfiguration,
              nodePermissioningConfigFile.orElse(getDefaultPermissioningFilePath(dataPath)),
              permissionsAccountsEnabled,
              accountPermissioningConfigFile.orElse(getDefaultPermissioningFilePath(dataPath)));

      localPermissioningConfigurationOptional = Optional.of(localPermissioningConfiguration);
    } else {
      if (nodePermissionsConfigFile != null && !permissionsNodesEnabled) {
        logger.warn(
            "Node permissioning config file set {} but no permissions enabled",
            nodePermissionsConfigFile);
      }

      if (accountPermissionsConfigFile != null && !permissionsAccountsEnabled) {
        logger.warn(
            "Account permissioning config file set {} but no permissions enabled",
            accountPermissionsConfigFile);
      }
      localPermissioningConfigurationOptional = Optional.empty();
    }

    final PermissioningConfiguration permissioningConfiguration =
        new PermissioningConfiguration(localPermissioningConfigurationOptional);

    return Optional.of(permissioningConfiguration);
  }

  private boolean localPermissionsEnabled() {
    return permissionsAccountsEnabled || permissionsNodesEnabled;
  }

  private String getDefaultPermissioningFilePath(final Path dataPath) {
    return dataPath
        + System.getProperty("file.separator")
        + DefaultCommandValues.PERMISSIONING_CONFIG_LOCATION;
  }
}
