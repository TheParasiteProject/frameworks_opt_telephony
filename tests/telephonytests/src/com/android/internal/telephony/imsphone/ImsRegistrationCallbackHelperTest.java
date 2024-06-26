/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.internal.telephony.imsphone;

import static android.telephony.ims.RegistrationManager.SUGGESTED_ACTION_NONE;
import static android.telephony.ims.RegistrationManager.SUGGESTED_ACTION_TRIGGER_PLMN_BLOCK;
import static android.telephony.ims.stub.ImsRegistrationImplBase.REGISTRATION_TECH_LTE;
import static android.telephony.ims.stub.ImsRegistrationImplBase.REGISTRATION_TECH_NONE;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.net.Uri;
import android.telephony.AccessNetworkConstants.AccessNetworkType;
import android.telephony.ims.ImsReasonInfo;
import android.telephony.ims.ImsRegistrationAttributes;
import android.telephony.ims.RegistrationManager;
import android.telephony.ims.RegistrationManager.RegistrationCallback;
import android.telephony.ims.stub.ImsRegistrationImplBase;

import androidx.test.filters.SmallTest;

import com.android.internal.telephony.TelephonyTest;
import com.android.internal.telephony.imsphone.ImsRegistrationCallbackHelper.ImsRegistrationUpdate;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ImsRegistrationCallbackHelperTest extends TelephonyTest {
    // Mocked classes
    private ImsRegistrationUpdate mMockRegistrationUpdate;

    private ImsRegistrationCallbackHelper mRegistrationCallbackHelper;

    @Before
    public void setUp() throws Exception {
        super.setUp(getClass().getSimpleName());
        mMockRegistrationUpdate = mock(ImsRegistrationUpdate.class);
        mRegistrationCallbackHelper = new ImsRegistrationCallbackHelper(mMockRegistrationUpdate,
                Runnable::run);
    }

    @After
    public void tearDown() throws Exception {
        mRegistrationCallbackHelper = null;
        super.tearDown();
    }

    @Test
    @SmallTest
    public void testRegistrationStateReset() {
        // When reset is called
        mRegistrationCallbackHelper.reset();

        // The registration state should be equal to REGISTRATION_STATE_NOT_REGISTERED
        assertEquals(RegistrationManager.REGISTRATION_STATE_NOT_REGISTERED,
                mRegistrationCallbackHelper.getImsRegistrationState());
    }

    @Test
    @SmallTest
    public void testRegistrationStateUpdate() {
        // Verify Registration state can be update to NOT registered correctly.
        int state = RegistrationManager.REGISTRATION_STATE_NOT_REGISTERED;
        mRegistrationCallbackHelper.updateRegistrationState(state);

        assertEquals(RegistrationManager.REGISTRATION_STATE_NOT_REGISTERED,
                mRegistrationCallbackHelper.getImsRegistrationState());

        // Verify Registration state can be update to registering correctly.
        state = RegistrationManager.REGISTRATION_STATE_REGISTERING;
        mRegistrationCallbackHelper.updateRegistrationState(state);

        assertEquals(RegistrationManager.REGISTRATION_STATE_REGISTERING,
                mRegistrationCallbackHelper.getImsRegistrationState());

        // Verify Registration state can be update to registered correctly.
        state = RegistrationManager.REGISTRATION_STATE_REGISTERED;
        mRegistrationCallbackHelper.updateRegistrationState(state);

        assertEquals(RegistrationManager.REGISTRATION_STATE_REGISTERED,
                mRegistrationCallbackHelper.getImsRegistrationState());
    }

    @Test
    @SmallTest
    public void testIsImsRegistered() {
        // When the registration state is not registered
        mRegistrationCallbackHelper.updateRegistrationState(
                RegistrationManager.REGISTRATION_STATE_NOT_REGISTERED);

        // The result of isImsRegistered should be false
        assertFalse(mRegistrationCallbackHelper.isImsRegistered());

        // When the registration state is not registered
        mRegistrationCallbackHelper.updateRegistrationState(
                RegistrationManager.REGISTRATION_STATE_REGISTERED);

        // The result of isImsRegistered should be true
        assertTrue(mRegistrationCallbackHelper.isImsRegistered());
    }

    @Test
    @SmallTest
    public void testImsOnRegistered() {
        // Verify the RegistrationCallback should not be null
        RegistrationCallback callback = mRegistrationCallbackHelper.getCallback();
        assertNotNull(callback);

        // When onRegistered is called, the registration state should be
        // REGISTRATION_STATE_REGISTERED
        ImsRegistrationAttributes attr = new ImsRegistrationAttributes.Builder(
                ImsRegistrationImplBase.REGISTRATION_TECH_IWLAN).build();
        callback.onRegistered(attr);

        assertEquals(RegistrationManager.REGISTRATION_STATE_REGISTERED,
                mRegistrationCallbackHelper.getImsRegistrationState());
        verify(mMockRegistrationUpdate).handleImsRegistered(attr);
    }

    @Test
    @SmallTest
    public void testImsOnRegistering() {
        // Verify the RegistrationCallback should not be null
        RegistrationCallback callback = mRegistrationCallbackHelper.getCallback();
        assertNotNull(callback);

        // When onRegistering is called, the registration state should be
        // REGISTRATION_STATE_REGISTERING
        callback.onRegistering(AccessNetworkType.IWLAN);
        // The registration state should be REGISTRATION_STATE_REGISTERING
        assertEquals(RegistrationManager.REGISTRATION_STATE_REGISTERING,
                mRegistrationCallbackHelper.getImsRegistrationState());
        verify(mMockRegistrationUpdate).handleImsRegistering(anyInt());
    }

    @Test
    @SmallTest
    public void testImsUnRegistered() {
        // Verify the RegistrationCallback should not be null
        RegistrationCallback callback = mRegistrationCallbackHelper.getCallback();
        assertNotNull(callback);

        // When onUnregistered is called, the registration state should be
        // REGISTRATION_STATE_NOT_REGISTERED
        ImsReasonInfo reasonInfo = new ImsReasonInfo(ImsReasonInfo.CODE_REGISTRATION_ERROR, 0);
        callback.onUnregistered(reasonInfo);
        // The registration state should be REGISTRATION_STATE_NOT_REGISTERED
        assertEquals(RegistrationManager.REGISTRATION_STATE_NOT_REGISTERED,
                mRegistrationCallbackHelper.getImsRegistrationState());
        verify(mMockRegistrationUpdate).handleImsUnregistered(eq(reasonInfo),
                eq(SUGGESTED_ACTION_NONE), eq(REGISTRATION_TECH_NONE));
    }

    @Test
    @SmallTest
    public void testImsUnRegisteredWithSuggestedAction() {
        // Verify the RegistrationCallback should not be null
        RegistrationCallback callback = mRegistrationCallbackHelper.getCallback();
        assertNotNull(callback);

        ImsReasonInfo reasonInfo = new ImsReasonInfo(ImsReasonInfo.CODE_REGISTRATION_ERROR, 0);
        callback.onUnregistered(reasonInfo, SUGGESTED_ACTION_TRIGGER_PLMN_BLOCK,
                REGISTRATION_TECH_LTE);

        assertEquals(RegistrationManager.REGISTRATION_STATE_NOT_REGISTERED,
                mRegistrationCallbackHelper.getImsRegistrationState());
        verify(mMockRegistrationUpdate).handleImsUnregistered(eq(reasonInfo),
                eq(SUGGESTED_ACTION_TRIGGER_PLMN_BLOCK), eq(REGISTRATION_TECH_LTE));
    }

    @Test
    @SmallTest
    public void testSubscriberAssociatedUriChanged() {
        // Verify the RegistrationCallback should not be null
        RegistrationCallback callback = mRegistrationCallbackHelper.getCallback();
        assertNotNull(callback);

        // When onSubscriberAssociatedUriChanged is called
        Uri[] uris = new Uri[0];
        callback.onSubscriberAssociatedUriChanged(uris);
        // The handleImsSubscriberAssociatedUriChanged should be called
        verify(mMockRegistrationUpdate).handleImsSubscriberAssociatedUriChanged(uris);
    }
}
