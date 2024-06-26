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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.provider.Telephony;
import android.test.mock.MockContentResolver;
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
public class CarrierActionAgentTest extends TelephonyTest {
    private CarrierActionAgent mCarrierActionAgentUT;
    private FakeContentResolver mFakeContentResolver;
    private static int DATA_CARRIER_ACTION_EVENT = 0;
    private static int RADIO_CARRIER_ACTION_EVENT = 1;

    // Mocked classes
    private Handler mDataActionHandler;
    private Handler mRadioActionHandler;

    private class FakeContentResolver extends MockContentResolver {
        @Override
        public void notifyChange(Uri uri, ContentObserver observer) {
            super.notifyChange(uri, observer);
            logd("onChanged(uri=" + uri + ")" + observer);
            if (observer != null) {
                observer.dispatchChange(false, uri);
            } else {
                mCarrierActionAgentUT.getContentObserver().dispatchChange(false, uri);
            }
        }
    }

    @Before
    public void setUp() throws Exception {
        logd("CarrierActionAgentTest +Setup!");
        super.setUp(getClass().getSimpleName());
        mDataActionHandler = mock(Handler.class);
        mRadioActionHandler = mock(Handler.class);
        mFakeContentResolver = new FakeContentResolver();
        doReturn(mFakeContentResolver).when(mContext).getContentResolver();
        mCarrierActionAgentUT = new CarrierActionAgent(mPhone);
        mCarrierActionAgentUT.registerForCarrierAction(
                CarrierActionAgent.CARRIER_ACTION_SET_METERED_APNS_ENABLED, mDataActionHandler,
                DATA_CARRIER_ACTION_EVENT, null, false);
        mCarrierActionAgentUT.registerForCarrierAction(
                CarrierActionAgent.CARRIER_ACTION_SET_RADIO_ENABLED, mRadioActionHandler,
                RADIO_CARRIER_ACTION_EVENT, null, false);
        processAllMessages();
        logd("CarrierActionAgentTest -Setup!");
    }

    @Test
    @SmallTest
    public void testCarrierActionResetOnAPM() {
        // setting observer register at sim loading
        final Intent intent = new Intent(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        intent.putExtra(IccCardConstants.INTENT_KEY_ICC_STATE,
                IccCardConstants.INTENT_VALUE_ICC_LOADED);
        intent.putExtra(PhoneConstants.PHONE_KEY, mPhone.getPhoneId());
        mContext.sendBroadcast(intent);
        processAllMessages();

        // no carrier actions triggered from sim loading since there are same as the current one
        ArgumentCaptor<Message> message = ArgumentCaptor.forClass(Message.class);
        verify(mDataActionHandler, times(0)).sendMessageAtTime(message.capture(), anyLong());
        verify(mRadioActionHandler, times(0)).sendMessageAtTime(message.capture(), anyLong());

        // disable metered apns and radio
        mCarrierActionAgentUT.carrierActionSetRadioEnabled(false);
        mCarrierActionAgentUT.carrierActionSetMeteredApnsEnabled(false);
        processAllMessages();
        verify(mDataActionHandler, times(1)).sendMessageAtTime(message.capture(), anyLong());
        assertEquals(DATA_CARRIER_ACTION_EVENT, message.getValue().what);
        assertEquals(false, ((AsyncResult) message.getValue().obj).result);
        verify(mRadioActionHandler, times(1)).sendMessageAtTime(message.capture(), anyLong());
        assertEquals(RADIO_CARRIER_ACTION_EVENT, message.getValue().what);
        assertEquals(false, ((AsyncResult) message.getValue().obj).result);

        // simulate APM change from off -> on
        Settings.Global.putInt(mFakeContentResolver, Settings.Global.AIRPLANE_MODE_ON, 1);
        mFakeContentResolver.notifyChange(
                Settings.Global.getUriFor(Settings.Global.AIRPLANE_MODE_ON), null);
        processAllMessages();

        // carrier actions triggered from APM
        verify(mDataActionHandler, times(2)).sendMessageAtTime(message.capture(), anyLong());
        assertEquals(DATA_CARRIER_ACTION_EVENT, message.getValue().what);
        assertEquals(true, ((AsyncResult) message.getValue().obj).result);

        verify(mRadioActionHandler, times(2)).sendMessageAtTime(message.capture(), anyLong());
        assertEquals(RADIO_CARRIER_ACTION_EVENT, message.getValue().what);
        assertEquals(true, ((AsyncResult) message.getValue().obj).result);
    }

    @Test
    @SmallTest
    public void testCarrierActionResetOnAPNChange() {
        // Setting observer register at sim loading
        final Intent intent = new Intent(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        intent.putExtra(IccCardConstants.INTENT_KEY_ICC_STATE,
                IccCardConstants.INTENT_VALUE_ICC_LOADED);
        intent.putExtra(PhoneConstants.PHONE_KEY, mPhone.getPhoneId());
        mContext.sendBroadcast(intent);
        processAllMessages();

        // no carrier actions triggered from sim loading since there are same as the current one
        ArgumentCaptor<Message> message = ArgumentCaptor.forClass(Message.class);
        verify(mDataActionHandler, times(0)).sendMessageAtTime(message.capture(), anyLong());
        verify(mRadioActionHandler, times(0)).sendMessageAtTime(message.capture(), anyLong());

        // disable metered apns and radio
        mCarrierActionAgentUT.carrierActionSetRadioEnabled(false);
        mCarrierActionAgentUT.carrierActionSetMeteredApnsEnabled(false);
        processAllMessages();

        verify(mDataActionHandler, times(1)).sendMessageAtTime(message.capture(), anyLong());
        assertEquals(DATA_CARRIER_ACTION_EVENT, message.getValue().what);
        assertEquals(false, ((AsyncResult) message.getValue().obj).result);

        verify(mRadioActionHandler, times(1)).sendMessageAtTime(message.capture(), anyLong());
        assertEquals(RADIO_CARRIER_ACTION_EVENT, message.getValue().what);
        assertEquals(false, ((AsyncResult) message.getValue().obj).result);

        // Simulate APN change
        mFakeContentResolver.notifyChange(Telephony.Carriers.CONTENT_URI, null);
        processAllMessages();

        // Carrier actions triggered from APN change
        verify(mDataActionHandler, times(2)).sendMessageAtTime(message.capture(), anyLong());
        assertEquals(DATA_CARRIER_ACTION_EVENT, message.getValue().what);
        assertEquals(true, ((AsyncResult) message.getValue().obj).result);

        verify(mRadioActionHandler, times(2)).sendMessageAtTime(message.capture(), anyLong());
        assertEquals(RADIO_CARRIER_ACTION_EVENT, message.getValue().what);
        assertEquals(true, ((AsyncResult) message.getValue().obj).result);
    }

    @After
    public void tearDown() throws Exception {
        Settings.Global.putInt(mFakeContentResolver, Settings.Global.AIRPLANE_MODE_ON, 0);
        mCarrierActionAgentUT = null;
        mFakeContentResolver = null;
        super.tearDown();
    }
}
