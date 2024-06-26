/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.telephony;

import android.os.Parcel;
import android.telephony.CellIdentityLte;
import android.telephony.CellInfo;
import android.test.AndroidTestCase;

import androidx.test.filters.SmallTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

/** Unit tests for {@link CellIdentityLte}. */

public class CellIdentityLteTest extends AndroidTestCase {
    private static final String LOG_TAG = "CellIdentityLteTest";

    // Cell identity ranges from 0 to 268435455.
    private static final int CI = 268435455;
    // Physical cell id ranges from 0 to 503.
    private static final int PCI = 503;
    // Tracking area code ranges from 0 to 65535.
    private static final int TAC = 65535;
    // Absolute RF Channel Number ranges from 0 to 262140.
    private static final int EARFCN = 262140;
    private static final int[] BANDS = new int[] {1, 2};
    private static final int MCC = 120;
    private static final int MNC = 260;
    private static final int BANDWIDTH = 5000;  // kHz
    private static final String MCC_STR = "120";
    private static final String MNC_STR = "260";
    private static final String ALPHA_LONG = "long";
    private static final String ALPHA_SHORT = "short";

    @SmallTest
    public void testDefaultConstructor() {
        CellIdentityLte ci =
                new CellIdentityLte(CI, PCI, TAC, EARFCN, BANDS, BANDWIDTH, MCC_STR, MNC_STR,
                        ALPHA_LONG, ALPHA_SHORT, Collections.emptyList(), null);

        assertEquals(CI, ci.getCi());
        assertEquals(PCI, ci.getPci());
        assertEquals(TAC, ci.getTac());
        assertEquals(EARFCN, ci.getEarfcn());
        assertEquals(EARFCN, ci.getChannelNumber());
        assertEquals(BANDWIDTH, ci.getBandwidth());
        assertEquals(MCC, ci.getMcc());
        assertEquals(MNC, ci.getMnc());
        assertEquals(MCC_STR, ci.getMccString());
        assertEquals(MNC_STR, ci.getMncString());
        assertEquals(MCC_STR + MNC_STR, ci.getMobileNetworkOperator());
        assertEquals(ALPHA_LONG, ci.getOperatorAlphaLong());
        assertEquals(ALPHA_SHORT, ci.getOperatorAlphaShort());

        String globalCi = MCC_STR + MNC_STR + Integer.toString(CI, 16);
        assertEquals(globalCi, ci.getGlobalCellId());
    }

    @SmallTest
    public void testConstructorWithThreeDigitMnc() {
        final String mncWithThreeDigit = "061";
        CellIdentityLte ci =
                new CellIdentityLte(CI, PCI, TAC, EARFCN, BANDS, BANDWIDTH, MCC_STR,
                        mncWithThreeDigit, ALPHA_LONG, ALPHA_SHORT, Collections.emptyList(), null);

        assertEquals(MCC, ci.getMcc());
        assertEquals(61, ci.getMnc());
        assertEquals(MCC_STR, ci.getMccString());
        assertEquals(mncWithThreeDigit, ci.getMncString());
        assertEquals(MCC_STR + mncWithThreeDigit, ci.getMobileNetworkOperator());
    }

    @SmallTest
    public void testConstructorWithTwoDigitMnc() {
        final String mncWithTwoDigit = "61";
        CellIdentityLte ci =
                new CellIdentityLte(CI, PCI, TAC, EARFCN, BANDS, BANDWIDTH, MCC_STR,
                        mncWithTwoDigit, ALPHA_LONG, ALPHA_SHORT, Collections.emptyList(), null);

        assertEquals(MCC, ci.getMcc());
        assertEquals(61, ci.getMnc());
        assertEquals(MCC_STR, ci.getMccString());
        assertEquals(mncWithTwoDigit, ci.getMncString());
        assertEquals(MCC_STR + mncWithTwoDigit, ci.getMobileNetworkOperator());
    }

    @SmallTest
    public void testConstructorWithEmptyMccMnc() {
        CellIdentityLte ci = new CellIdentityLte(
                CI, PCI, TAC, EARFCN, BANDS, BANDWIDTH, null, null, ALPHA_LONG,
                ALPHA_SHORT, Collections.emptyList(), null);

        assertEquals(Integer.MAX_VALUE, ci.getMcc());
        assertEquals(Integer.MAX_VALUE, ci.getMnc());
        assertNull(ci.getMccString());
        assertNull(ci.getMncString());
        assertNull(ci.getMobileNetworkOperator());

        ci = new CellIdentityLte(
                CI, PCI, TAC, EARFCN, BANDS, BANDWIDTH, MCC_STR, null, ALPHA_LONG, ALPHA_SHORT,
                Collections.emptyList(), null);

        assertEquals(MCC, ci.getMcc());
        assertEquals(Integer.MAX_VALUE, ci.getMnc());
        assertEquals(MCC_STR, ci.getMccString());
        assertNull(ci.getMncString());
        assertNull(ci.getMobileNetworkOperator());

        ci = new CellIdentityLte(
                CI, PCI, TAC, EARFCN, BANDS, BANDWIDTH, null, MNC_STR, ALPHA_LONG, ALPHA_SHORT,
                Collections.emptyList(), null);

        assertEquals(MNC, ci.getMnc());
        assertEquals(Integer.MAX_VALUE, ci.getMcc());
        assertEquals(MNC_STR, ci.getMncString());
        assertNull(ci.getMccString());
        assertNull(ci.getMobileNetworkOperator());

        ci = new CellIdentityLte(
                CI, PCI, TAC, EARFCN, BANDS, BANDWIDTH, "", "", ALPHA_LONG, ALPHA_SHORT,
                Collections.emptyList(), null);

        assertEquals(Integer.MAX_VALUE, ci.getMcc());
        assertEquals(Integer.MAX_VALUE, ci.getMnc());
        assertNull(ci.getMccString());
        assertNull(ci.getMncString());
        assertNull(ci.getMobileNetworkOperator());
    }

    @SmallTest
    public void testFormerConstructor() {
        CellIdentityLte ci =
                new CellIdentityLte(MCC, MNC, CI, PCI, TAC);

        assertEquals(CI, ci.getCi());
        assertEquals(PCI, ci.getPci());
        assertEquals(TAC, ci.getTac());
        assertEquals(Integer.MAX_VALUE, ci.getEarfcn());
        assertEquals(Integer.MAX_VALUE, ci.getBandwidth());
        assertEquals(MCC, ci.getMcc());
        assertEquals(MNC, ci.getMnc());
        assertEquals(MCC_STR, ci.getMccString());
        assertEquals(MNC_STR, ci.getMncString());
        assertEquals(MCC_STR + MNC_STR, ci.getMobileNetworkOperator());
        assertNull(ci.getOperatorAlphaLong());
        assertNull(ci.getOperatorAlphaShort());
    }

    @SmallTest
    public void testEquals() {
        CellIdentityLte ciA = new CellIdentityLte(
                CI, PCI, TAC, EARFCN, BANDS, BANDWIDTH, MCC_STR, MNC_STR, ALPHA_LONG, ALPHA_SHORT,
                Collections.emptyList(), null);
        CellIdentityLte ciB = new CellIdentityLte(
                CI, PCI, TAC, EARFCN, BANDS, BANDWIDTH, MCC_STR, MNC_STR, ALPHA_LONG, ALPHA_SHORT,
                Collections.emptyList(), null);

        assertTrue(ciA.equals(ciB));

        ciA = new CellIdentityLte(
                CI, PCI, TAC, EARFCN, BANDS, BANDWIDTH, null, null, ALPHA_LONG, ALPHA_SHORT,
                Collections.emptyList(), null);
        ciB = new CellIdentityLte(
                CI, PCI, TAC, EARFCN, BANDS, BANDWIDTH, null, null, ALPHA_LONG, ALPHA_SHORT,
                Collections.emptyList(), null);

        assertTrue(ciA.equals(ciB));

        ciA = new CellIdentityLte(
                CI, PCI, TAC, EARFCN, BANDS, BANDWIDTH, MCC_STR, null, ALPHA_LONG, ALPHA_SHORT,
                Collections.emptyList(), null);
        ciB = new CellIdentityLte(
                CI, PCI, TAC, EARFCN, BANDS, BANDWIDTH, null, null, ALPHA_LONG, ALPHA_SHORT,
                Collections.emptyList(), null);

        assertFalse(ciA.equals(ciB));
    }

    @SmallTest
    public void testParcel() {
        CellIdentityLte ci =
                new CellIdentityLte(CI, PCI, TAC, EARFCN, BANDS, BANDWIDTH, MCC_STR, MNC_STR,
                        ALPHA_LONG, ALPHA_SHORT, Collections.emptyList(), null);

        Parcel p = Parcel.obtain();
        ci.writeToParcel(p, 0);
        p.setDataPosition(0);

        CellIdentityLte newCi = CellIdentityLte.CREATOR.createFromParcel(p);
        assertEquals(ci, newCi);
    }

    @SmallTest
    public void testParcelWithUnknownMccMnc() {
        CellIdentityLte ci = new CellIdentityLte(
                CI, PCI, TAC, EARFCN, BANDS, BANDWIDTH, null, null, ALPHA_LONG, ALPHA_SHORT,
                Collections.emptyList(), null);

        Parcel p = Parcel.obtain();
        p.writeInt(CellInfo.TYPE_LTE);
        p.writeString(String.valueOf(Integer.MAX_VALUE));
        p.writeString(String.valueOf(Integer.MAX_VALUE));
        p.writeString(ALPHA_LONG);
        p.writeString(ALPHA_SHORT);
        p.writeInt(CI);
        p.writeInt(PCI);
        p.writeInt(TAC);
        p.writeInt(EARFCN);
        p.writeIntArray(BANDS);
        p.writeInt(BANDWIDTH);
        p.setDataPosition(0);

        CellIdentityLte newCi = CellIdentityLte.CREATOR.createFromParcel(p);
        assertEquals(ci, newCi);
    }

    @SmallTest
    public void testParcelWithInvalidMccMnc() {
        final String invalidMcc = "randomStuff";
        final String invalidMnc = "randomStuff";
        CellIdentityLte ci = new CellIdentityLte(
                CI, PCI, TAC, EARFCN, BANDS, BANDWIDTH, null, null, ALPHA_LONG, ALPHA_SHORT,
                Collections.emptyList(), null);

        Parcel p = Parcel.obtain();
        p.writeInt(CellInfo.TYPE_LTE);
        p.writeString(invalidMcc);
        p.writeString(invalidMnc);
        p.writeString(ALPHA_LONG);
        p.writeString(ALPHA_SHORT);
        p.writeInt(CI);
        p.writeInt(PCI);
        p.writeInt(TAC);
        p.writeInt(EARFCN);
        p.writeIntArray(BANDS);
        p.writeInt(BANDWIDTH);
        p.setDataPosition(0);

        CellIdentityLte newCi = CellIdentityLte.CREATOR.createFromParcel(p);
        assertEquals(ci, newCi);
    }

    @SmallTest
    public void testBands() {
        android.hardware.radio.V1_5.CellIdentityLte cid =
                new android.hardware.radio.V1_5.CellIdentityLte();
        cid.bands = Arrays.stream(BANDS).boxed().collect(Collectors.toCollection(ArrayList::new));

        CellIdentityLte cellIdentityLte = RILUtils.convertHalCellIdentityLte(cid);
        assertTrue(Arrays.equals(cellIdentityLte.getBands(), BANDS));

    }
}
