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

package android.telephony.ims;

import static android.telephony.ims.RegistrationManager.SUGGESTED_ACTION_TRIGGER_PLMN_BLOCK;
import static android.telephony.ims.stub.ImsRegistrationImplBase.REGISTRATION_TECH_LTE;

import static junit.framework.Assert.assertEquals;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.net.Uri;
import android.os.Parcel;
import android.os.RemoteException;
import android.telephony.ims.aidl.IImsRegistration;
import android.telephony.ims.aidl.IImsRegistrationCallback;
import android.telephony.ims.feature.ImsFeature;
import android.telephony.ims.stub.ImsFeatureConfiguration;
import android.telephony.ims.stub.ImsRegistrationImplBase;
import android.util.ArraySet;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.AdditionalMatchers;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

@RunWith(AndroidJUnit4.class)
public class ImsRegistrationTests {

    @Spy private IImsRegistrationCallback.Stub mCallback;
    @Spy private IImsRegistrationCallback.Stub mCallback2;
    private ImsRegistrationImplBase mRegistration;
    private IImsRegistration mRegBinder;

    @Before
    public void setup() throws RemoteException {
        MockitoAnnotations.initMocks(this);
        mRegistration = new ImsRegistrationImplBase(Runnable::run);
        mRegBinder = mRegistration.getBinder();
        mRegBinder.addRegistrationCallback(mCallback);
    }

    @After
    public void tearDown() {
        mRegistration = null;
        mRegBinder = null;
    }

    @SmallTest
    @Test
    public void testRegistrationConfigParcel() {
        ImsFeatureConfiguration testConfig = new ImsFeatureConfiguration.Builder()
                .addFeature(/*slotId*/ 0, ImsFeature.FEATURE_MMTEL)
                .addFeature(/*slotId*/ 0, ImsFeature.FEATURE_RCS)
                .build();
        Parcel p = Parcel.obtain();
        testConfig.writeToParcel(p, 0);
        p.setDataPosition(0);
        ImsFeatureConfiguration result =
                ImsFeatureConfiguration.CREATOR.createFromParcel(p);
        p.recycle();

        assertEquals(testConfig, result);
    }

    @SmallTest
    @Test
    public void testRegistrationConfigPermutationEqual() {
        ImsFeatureConfiguration testConfig = new ImsFeatureConfiguration.Builder()
                .addFeature(/*slotId*/ 0, ImsFeature.FEATURE_MMTEL)
                .addFeature(/*slotId*/ 0, ImsFeature.FEATURE_RCS)
                .build();

        // Permute field insertion ordering to ensure order doesn't matter.
        ImsFeatureConfiguration testConfig2 = new ImsFeatureConfiguration.Builder()
                .addFeature(/*slotId*/ 0, ImsFeature.FEATURE_RCS)
                .addFeature(/*slotId*/ 0, ImsFeature.FEATURE_MMTEL)
                .build();

        assertEquals(testConfig, testConfig2);
    }

    @SmallTest
    @Test
    public void testRegistrationConfigConstructorsEqual() {
        // Permute field insertion ordering to ensure order doesn't matter.
        ImsFeatureConfiguration testConfig = new ImsFeatureConfiguration.Builder()
                .addFeature(/*slotId*/ 0, ImsFeature.FEATURE_MMTEL)
                .addFeature(/*slotId*/ 0, ImsFeature.FEATURE_RCS)
                .build();

        // Permute field insertion ordering to ensure order doesn't matter.
        ImsFeatureConfiguration testConfig2 = new ImsFeatureConfiguration.Builder()
                .addFeature(/*slotId*/ 0, ImsFeature.FEATURE_RCS)
                .addFeature(/*slotId*/ 0, ImsFeature.FEATURE_MMTEL)
                .build();

        assertEquals(testConfig, testConfig2);
    }

    @SmallTest
    @Test
    public void testRegistrationCallbackOnRegistered() throws RemoteException {
        final ArraySet<String> features = new ArraySet<>();
        features.add("feature1");
        features.add("feature2");
        ImsRegistrationAttributes attr = new ImsRegistrationAttributes.Builder(
                ImsRegistrationImplBase.REGISTRATION_TECH_LTE).setFeatureTags(features).build();
        mRegistration.onRegistered(attr);

        verify(mCallback).onRegistered(attr);
    }

    @SmallTest
    @Test
    public void testRegistrationCallbackOnRegistering() throws RemoteException {
        ImsRegistrationAttributes attr = new ImsRegistrationAttributes.Builder(
                ImsRegistrationImplBase.REGISTRATION_TECH_LTE).build();
        mRegistration.onRegistering(ImsRegistrationImplBase.REGISTRATION_TECH_LTE);

        verify(mCallback).onRegistering(attr);
    }

    @SmallTest
    @Test
    public void testRegistrationCallbackOnDeregistered() throws RemoteException {
        ImsReasonInfo info = new ImsReasonInfo();
        mRegistration.onDeregistered(info);

        verify(mCallback).onDeregistered(eq(info), anyInt(), anyInt());
    }

    @SmallTest
    @Test
    public void testRegistrationCallbackOnDeregisteredWithSuggestedAction() throws RemoteException {
        ImsReasonInfo info = new ImsReasonInfo();
        mRegistration.onDeregistered(info,
                SUGGESTED_ACTION_TRIGGER_PLMN_BLOCK, REGISTRATION_TECH_LTE);

        verify(mCallback).onDeregistered(eq(info),
                eq(SUGGESTED_ACTION_TRIGGER_PLMN_BLOCK), eq(REGISTRATION_TECH_LTE));
    }

    @SmallTest
    @Test
    public void testRegistrationCallbackOnDeregisteredWithRegistrationAttributes()
            throws RemoteException {
        ImsReasonInfo info = new ImsReasonInfo();
        SipDetails details = new SipDetails.Builder(SipDetails.METHOD_REGISTER)
                .setCSeq(1).setSipResponseCode(200, "OK")
                .setSipResponseReasonHeader(10, "reasonText")
                .setCallId("testCallId").build();

        mRegistration.onDeregistered(info, details);

        verify(mCallback).onDeregisteredWithDetails(eq(info), anyInt(), anyInt(), eq(details));
    }

    @SmallTest
    @Test
    public void testRegistrationCallbackOnTechChangeFailed() throws RemoteException {
        ImsReasonInfo info = new ImsReasonInfo();
        mRegistration.onTechnologyChangeFailed(ImsRegistrationImplBase.REGISTRATION_TECH_IWLAN,
                info);

        verify(mCallback).onTechnologyChangeFailed(
                eq(ImsRegistrationImplBase.REGISTRATION_TECH_IWLAN), eq(info));
    }

    @SmallTest
    @Test
    public void testSubscriberUrisChanged() throws RemoteException {
        Uri[] uris = new Uri[1];
        uris[0] = Uri.fromParts("tel", "5555551212", null);

        mRegistration.onSubscriberAssociatedUriChanged(uris);

        verify(mCallback).onSubscriberAssociatedUriChanged(eq(uris));
    }

    @SmallTest
    @Test
    public void testRegistrationCallbackAfterUnregistered() throws RemoteException {
        mRegBinder.removeRegistrationCallback(mCallback);

        ImsRegistrationAttributes attr = new ImsRegistrationAttributes.Builder(
                ImsRegistrationImplBase.REGISTRATION_TECH_LTE).build();
        mRegistration.onRegistered(attr);

        verify(mCallback, never()).onRegistered(attr);
    }

    @SmallTest
    @Test
    public void testRegistrationCallbackSendCurrentState() throws RemoteException {
        final ArraySet<String> features = new ArraySet<>();
        features.add("feature1");
        features.add("feature2");
        ImsRegistrationAttributes attr = new ImsRegistrationAttributes.Builder(
                ImsRegistrationImplBase.REGISTRATION_TECH_LTE).setFeatureTags(features).build();
        mRegistration.onRegistered(attr);

        mRegBinder.addRegistrationCallback(mCallback2);

        verify(mCallback2).onRegistered(attr);
    }

    @SmallTest
    @Test
    public void testRegistrationCallbackGetRegistrationTech() throws RemoteException {
        mRegistration.onRegistered(ImsRegistrationImplBase.REGISTRATION_TECH_LTE);

        assertEquals(ImsRegistrationImplBase.REGISTRATION_TECH_LTE,
                mRegBinder.getRegistrationTechnology());
    }

    @SmallTest
    @Test
    public void testRegistrationCallbackSendCurrentStateDisconnected() throws RemoteException {
        ImsReasonInfo info = new ImsReasonInfo(ImsReasonInfo.CODE_LOCAL_NETWORK_NO_LTE_COVERAGE, 0);
        mRegistration.onDeregistered(info);

        mRegBinder.addRegistrationCallback(mCallback2);

        // The original callback that has been registered should get LTE tech in disconnected
        // message
        verify(mCallback).onDeregistered(eq(info), anyInt(), anyInt());
        // A callback that has just been registered should get NONE for tech in disconnected
        // message
        verify(mCallback2).onDeregistered(eq(info), anyInt(), anyInt());
    }

    @SmallTest
    @Test
    public void testRegistrationCallbackGetRegistrationTechDisconnected() throws RemoteException {
        ImsReasonInfo info = new ImsReasonInfo(ImsReasonInfo.CODE_LOCAL_NETWORK_NO_LTE_COVERAGE, 0);

        mRegistration.onDeregistered(info);

        verify(mCallback).onDeregistered(eq(info), anyInt(), anyInt());
        assertEquals(ImsRegistrationImplBase.REGISTRATION_TECH_NONE,
                mRegBinder.getRegistrationTechnology());
    }

    @SmallTest
    @Test
    public void testRegistrationCallbackNoCallbackIfUnknown() throws RemoteException {
        mRegBinder.addRegistrationCallback(mCallback2);
        // Verify that if we have never set the registration state, we do not callback immediately
        // with onUnregistered.
        verify(mCallback2, never()).onDeregistered(any(ImsReasonInfo.class), anyInt(), anyInt());
    }

    @SmallTest
    @Test
    public void testRegistrationCallbackCalledOnAdd() throws RemoteException {
        mRegistration.onSubscriberAssociatedUriChanged(new Uri[] { null, null });

        mRegBinder.addRegistrationCallback(mCallback2);

        verify(mCallback2).onSubscriberAssociatedUriChanged(
                AdditionalMatchers.aryEq(new Uri[] { null, null }));
    }

    @SmallTest
    @Test
    public void testRegistrationCallbackNotCalledOnAddAfterDeregistered() throws RemoteException {
        ImsReasonInfo info = new ImsReasonInfo(ImsReasonInfo.CODE_LOCAL_NETWORK_NO_LTE_COVERAGE, 0);
        mRegistration.onSubscriberAssociatedUriChanged(new Uri[] { null, null });

        mRegistration.onDeregistered(info);
        mRegBinder.addRegistrationCallback(mCallback2);

        verify(mCallback2, never()).onSubscriberAssociatedUriChanged(any());
    }

    @SmallTest
    @Test
    public void testRegistrationCallbackNotCalledOnAddAndNoSubscriberChanged()
            throws RemoteException {
        mRegBinder.addRegistrationCallback(mCallback2);
        verify(mCallback2, never()).onSubscriberAssociatedUriChanged(any());
    }
}
