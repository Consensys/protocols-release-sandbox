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
package org.hyperledger.besu.ethereum.mainnet.feemarket;

import static org.assertj.core.api.Assertions.assertThat;

import org.hyperledger.besu.crypto.KeyPair;
import org.hyperledger.besu.crypto.SignatureAlgorithmFactory;
import org.hyperledger.besu.datatypes.BlobGas;
import org.hyperledger.besu.datatypes.TransactionType;
import org.hyperledger.besu.datatypes.Wei;
import org.hyperledger.besu.ethereum.core.Transaction;
import org.hyperledger.besu.ethereum.core.TransactionTestFixture;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FixedBaseFeeMarketTest {

  private static final KeyPair KEY_PAIR1 =
      SignatureAlgorithmFactory.getInstance().generateKeyPair();
  private static final long FORK_BLOCK = 0;
  private FixedBaseFeeMarket fixedBaseFeeMarket;

  @BeforeEach
  public void setUp() throws Exception {
    fixedBaseFeeMarket = new FixedBaseFeeMarket(FORK_BLOCK, Wei.ONE);
  }

  @Test
  public void getBasefeeMaxChangeDenominatorShouldUseLondonDefault() {
    assertThat(fixedBaseFeeMarket.getBasefeeMaxChangeDenominator())
        .isEqualTo(LondonFeeMarket.DEFAULT_BASEFEE_MAX_CHANGE_DENOMINATOR);
  }

  @Test
  public void getInitialBasefeeShouldBeZero() {
    assertThat(fixedBaseFeeMarket.getInitialBasefee()).isEqualTo(Wei.ONE);
  }

  @Test
  public void getSlackCoefficientShouldUseLondonDefault() {
    assertThat(fixedBaseFeeMarket.getSlackCoefficient())
        .isEqualTo(LondonFeeMarket.DEFAULT_SLACK_COEFFICIENT);
  }

  @Test
  public void getTransactionPriceCalculatorShouldBeEIP1559() {
    // only eip1559 will read the fee per gas values
    final Transaction transaction =
        new TransactionTestFixture()
            .type(TransactionType.EIP1559)
            .maxFeePerGas(Optional.of(Wei.of(8)))
            .maxPriorityFeePerGas(Optional.of(Wei.of(8)))
            .gasPrice(null)
            .createTransaction(KEY_PAIR1);

    assertThat(
            fixedBaseFeeMarket
                .getTransactionPriceCalculator()
                .price(transaction, Optional.of(Wei.ZERO)))
        .isEqualTo(Wei.of(8));
  }

  @Test
  public void satisfiesFloorTxCostWhenGasFeeIsNonZero() {
    final Transaction transaction =
        new TransactionTestFixture()
            .type(TransactionType.FRONTIER)
            .gasPrice(Wei.of(7))
            .createTransaction(KEY_PAIR1);
    assertThat(fixedBaseFeeMarket.satisfiesFloorTxFee(transaction)).isTrue();
  }

  @Test
  public void satisfiesFloorTxCostWhenGasFeeIsZero() {
    final Transaction transaction =
        new TransactionTestFixture()
            .type(TransactionType.EIP1559)
            .maxFeePerGas(Optional.of(Wei.ZERO))
            .maxPriorityFeePerGas(Optional.of(Wei.ZERO))
            .gasPrice(null)
            .createTransaction(KEY_PAIR1);
    assertThat(fixedBaseFeeMarket.satisfiesFloorTxFee(transaction)).isFalse();
  }

  @Test
  public void computeBaseFeeReturnsFixedValue() {
    assertThat(fixedBaseFeeMarket.computeBaseFee(1L, Wei.of(1), 1L, 2L)).isEqualTo(Wei.ONE);
  }

  @Test
  public void baseFeeValidationModeShouldBeNoneWhenIsForkBlock() {
    assertThat(fixedBaseFeeMarket.baseFeeValidationMode(FORK_BLOCK))
        .isEqualTo(BaseFeeMarket.ValidationMode.NONE);
  }

  @Test
  public void baseFeeValidationModeShouldBeNoneWhenIsNotForkBlock() {
    assertThat(fixedBaseFeeMarket.baseFeeValidationMode(FORK_BLOCK + 1))
        .isEqualTo(BaseFeeMarket.ValidationMode.NONE);
  }

  @Test
  public void gasLimitValidationModeShouldBeInitialWhenIsForkBlock() {
    assertThat(fixedBaseFeeMarket.gasLimitValidationMode(FORK_BLOCK))
        .isEqualTo(BaseFeeMarket.ValidationMode.INITIAL);
  }

  @Test
  public void gasLimitValidationModeShouldBeOngoingWhenIsNotForkBlock() {
    assertThat(fixedBaseFeeMarket.gasLimitValidationMode(FORK_BLOCK + 1))
        .isEqualTo(BaseFeeMarket.ValidationMode.ONGOING);
  }

  @Test
  public void isBeforeForkBlockShouldBeTrue() {
    final FixedBaseFeeMarket fixedBaseFeeMarket = new FixedBaseFeeMarket(10, Wei.ONE);
    assertThat(fixedBaseFeeMarket.isBeforeForkBlock(9)).isTrue();
  }

  @Test
  public void isBeforeForkBlockShouldBeFalse() {
    final FixedBaseFeeMarket fixedBaseFeeMarket = new FixedBaseFeeMarket(10, Wei.ONE);
    assertThat(fixedBaseFeeMarket.isBeforeForkBlock(10)).isFalse();
    assertThat(fixedBaseFeeMarket.isBeforeForkBlock(11)).isFalse();
  }

  @Test
  public void implementsBlobFeeShouldReturnFalse() {
    assertThat(fixedBaseFeeMarket.implementsBlobFee()).isFalse();
  }

  @Test
  public void dataPriceShouldReturnsZero() {
    assertThat(fixedBaseFeeMarket.blobGasPricePerGas(BlobGas.ONE)).isEqualTo(Wei.ZERO);
  }
}
