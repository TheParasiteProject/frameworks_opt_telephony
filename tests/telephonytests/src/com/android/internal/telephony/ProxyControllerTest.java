/*
 * Copyright (C) 2019 The Android Open Source Project
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

import static android.telephony.RadioAccessFamily.RAF_GSM;
import static android.telephony.RadioAccessFamily.RAF_LTE;

import static com.android.internal.telephony.ProxyController.EVENT_FINISH_RC_RESPONSE;
import static com.android.internal.telephony.ProxyController.EVENT_MULTI_SIM_CONFIG_CHANGED;
import static com.android.internal.telephony.ProxyController.EVENT_START_RC_RESPONSE;
import static com.android.internal.telephony.ProxyController.EVENT_TIMEOUT;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.telephony.RadioAccessFamily;
import android.testing.AndroidTestingRunner;
import android.testing.TestableLooper;

import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

@RunWith(AndroidTestingRunner.class)
@TestableLooper.RunWithLooper
public class ProxyControllerTest extends TelephonyTest {
    // Mocked classes
    ProxyController mProxyController;

    @Before
    public void setUp() throws Exception {
        super.setUp(getClass().getSimpleName());
        replaceInstance(ProxyController.class, "sProxyController", null, null);
        mProxyController = new ProxyController(mContext, mFeatureFlags);
    }

    @After
    public void tearDown() throws Exception {
        mProxyController = null;
        super.tearDown();
    }

    @Test
    @SmallTest
    public void testMultiSimConfigChange() throws Exception {
        ArgumentCaptor<Handler> handlerCaptor = ArgumentCaptor.forClass(Handler.class);
        ArgumentCaptor<Integer> intCaptor = ArgumentCaptor.forClass(int.class);

        // Switch to dual-SIM and send multi sim config change callback.
        replaceInstance(PhoneFactory.class, "sPhones", null, new Phone[] {mPhone, mPhone2});
        Message.obtain(mProxyController.mHandler, EVENT_MULTI_SIM_CONFIG_CHANGED).sendToTarget();
        processAllMessages();
        verify(mPhone2).registerForRadioCapabilityChanged(any(), anyInt(), any());

        // Switch to single-SIM and verify there's at least no crash.
        replaceInstance(PhoneFactory.class, "sPhones", null, new Phone[] {mPhone});
        Message.obtain(mProxyController.mHandler, EVENT_MULTI_SIM_CONFIG_CHANGED).sendToTarget();
        processAllMessages();
    }

    @Test
    @SmallTest
    public void testRequestNotSupported() throws Exception {
        int activeModemCount = 2;
        replaceInstance(PhoneFactory.class, "sPhones", null, new Phone[] {mPhone, mPhone2});
        doReturn(activeModemCount).when(mTelephonyManager).getPhoneCount();
        doReturn(RAF_GSM | RAF_LTE).when(mPhone).getRadioAccessFamily();
        doReturn(RAF_GSM).when(mPhone2).getRadioAccessFamily();

        Message.obtain(mProxyController.mHandler, EVENT_MULTI_SIM_CONFIG_CHANGED).sendToTarget();
        processAllMessages();
        verify(mPhone2).registerForRadioCapabilityChanged(any(), anyInt(), any());

        RadioAccessFamily[] rafs = new RadioAccessFamily[activeModemCount];
        rafs[0] = new RadioAccessFamily(0, RAF_GSM);
        rafs[1] = new RadioAccessFamily(1, RAF_GSM | RAF_LTE);
        mProxyController.setRadioCapability(rafs);

        Message.obtain(
                        mProxyController.mHandler,
                        EVENT_START_RC_RESPONSE,
                        new AsyncResult(
                                null,
                                null,
                                new CommandException(CommandException.Error.REQUEST_NOT_SUPPORTED)))
                .sendToTarget();
        processAllMessages();

        assertFalse(mProxyController.isWakeLockHeld());
    }

    @Test
    @SmallTest
    public void testWithNonPermanentExceptionOnRCResponse_WithExceptionOnFinishResponse()
            throws Exception {
        int activeModemCount = 2;
        replaceInstance(PhoneFactory.class, "sPhones", null, new Phone[] {mPhone, mPhone2});
        doReturn(activeModemCount).when(mTelephonyManager).getPhoneCount();
        doReturn(RAF_GSM | RAF_LTE).when(mPhone).getRadioAccessFamily();
        doReturn(RAF_GSM).when(mPhone2).getRadioAccessFamily();

        Message.obtain(mProxyController.mHandler, EVENT_MULTI_SIM_CONFIG_CHANGED).sendToTarget();
        processAllMessages();
        verify(mPhone2).registerForRadioCapabilityChanged(any(), anyInt(), any());

        RadioAccessFamily[] rafs = new RadioAccessFamily[activeModemCount];
        rafs[0] = new RadioAccessFamily(0, RAF_GSM);
        rafs[1] = new RadioAccessFamily(1, RAF_GSM | RAF_LTE);
        mProxyController.setRadioCapability(rafs);

        Message.obtain(
                        mProxyController.mHandler,
                        EVENT_START_RC_RESPONSE,
                        new AsyncResult(
                                null,
                                null,
                                new CommandException(CommandException.Error.RADIO_NOT_AVAILABLE)))
                .sendToTarget();
        processAllMessages();
        assertTrue(mProxyController.isWakeLockHeld());
        onFinishResponseWithException();
    }

    @Test
    @SmallTest
    public void testWithNonPermanentExceptionOnRCResponse_WithoutExceptionOnFinishResponse()
            throws Exception {
        int activeModemCount = 2;
        replaceInstance(PhoneFactory.class, "sPhones", null, new Phone[] {mPhone, mPhone2});
        doReturn(activeModemCount).when(mTelephonyManager).getPhoneCount();
        doReturn(RAF_GSM | RAF_LTE).when(mPhone).getRadioAccessFamily();
        doReturn(RAF_GSM).when(mPhone2).getRadioAccessFamily();

        Message.obtain(mProxyController.mHandler, EVENT_MULTI_SIM_CONFIG_CHANGED).sendToTarget();
        processAllMessages();
        verify(mPhone2).registerForRadioCapabilityChanged(any(), anyInt(), any());

        RadioAccessFamily[] rafs = new RadioAccessFamily[activeModemCount];
        rafs[0] = new RadioAccessFamily(0, RAF_GSM);
        rafs[1] = new RadioAccessFamily(1, RAF_GSM | RAF_LTE);
        mProxyController.setRadioCapability(rafs);

        Message.obtain(
                        mProxyController.mHandler,
                        EVENT_START_RC_RESPONSE,
                        new AsyncResult(null, null, null))
                .sendToTarget();
        processAllMessages();
        assertTrue(mProxyController.isWakeLockHeld());
        onFinishResponseWithoutException();
    }

    @Test
    @SmallTest
    public void testOnRCResponseTimeout_WithExceptionOnFinishResponse() throws Exception {
        int activeModemCount = 2;
        replaceInstance(PhoneFactory.class, "sPhones", null, new Phone[] {mPhone, mPhone2});
        doReturn(activeModemCount).when(mTelephonyManager).getPhoneCount();
        doReturn(RAF_GSM | RAF_LTE).when(mPhone).getRadioAccessFamily();
        doReturn(RAF_GSM).when(mPhone2).getRadioAccessFamily();

        Message.obtain(mProxyController.mHandler, EVENT_MULTI_SIM_CONFIG_CHANGED).sendToTarget();
        processAllMessages();
        verify(mPhone2).registerForRadioCapabilityChanged(any(), anyInt(), any());

        RadioAccessFamily[] rafs = new RadioAccessFamily[activeModemCount];
        rafs[0] = new RadioAccessFamily(0, RAF_GSM);
        rafs[1] = new RadioAccessFamily(1, RAF_GSM | RAF_LTE);
        mProxyController.setRadioCapability(rafs);

        Message.obtain(
                        mProxyController.mHandler,
                        EVENT_TIMEOUT,
                        new AsyncResult(
                                null,
                                null,
                                new CommandException(CommandException.Error.REQUEST_NOT_SUPPORTED)))
                .sendToTarget();
        processAllMessages();
        onFinishResponseWithException();
    }

    private void onFinishResponseWithException() throws Exception {
        replaceInstance(
                ProxyController.class, "mRadioAccessFamilyStatusCounter", mProxyController, 1);
        replaceInstance(ProxyController.class, "mTransactionFailed", mProxyController, true);
        Message.obtain(
                        mProxyController.mHandler,
                        EVENT_FINISH_RC_RESPONSE,
                        new AsyncResult(
                                null,
                                null,
                                new CommandException(CommandException.Error.REQUEST_NOT_SUPPORTED)))
                .sendToTarget();
        processAllMessages();
        assertTrue(mProxyController.isWakeLockHeld());
    }

    private void onFinishResponseWithoutException() throws Exception {
        replaceInstance(
                ProxyController.class, "mRadioAccessFamilyStatusCounter", mProxyController, 1);
        replaceInstance(ProxyController.class, "mTransactionFailed", mProxyController, false);
        replaceInstance(
                ProxyController.class, "mRadioCapabilitySessionId", mProxyController, 123456);
        Message.obtain(
                        mProxyController.mHandler,
                        EVENT_FINISH_RC_RESPONSE,
                        new AsyncResult(
                                null, new RadioCapability(0, 123456, 0, 0, "test_modem", 0), null))
                .sendToTarget();
        processAllMessages();
        assertFalse(mProxyController.isWakeLockHeld());
    }
}
