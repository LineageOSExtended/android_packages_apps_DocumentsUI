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

package com.android.documentsui;

import static com.android.documentsui.base.Providers.AUTHORITY_STORAGE;

import static com.google.common.truth.Truth.assertThat;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.DocumentsContract;

import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import com.android.documentsui.base.DocumentInfo;
import com.android.documentsui.picker.PickActivity;
import com.android.documentsui.testing.TestProvidersAccess;
import com.android.documentsui.ui.TestDialogController;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

@SmallTest
public class PickActivityTest {

    private static final String RESULT_EXTRA = "test_result_extra";
    private static final String RESULT_DATA = "123321";

    private Context mTargetContext;
    private Intent intentGetContent;
    private TestDialogController testDialogs;

    @Rule
    public final ActivityTestRule<PickActivity> mRule =
            new ActivityTestRule<>(PickActivity.class, false, false);

    @Before
    public void setUp() throws Exception {
        mTargetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        intentGetContent = new Intent(Intent.ACTION_GET_CONTENT);
        intentGetContent.addCategory(Intent.CATEGORY_OPENABLE);
        intentGetContent.setType("*/*");
        Uri hintUri = DocumentsContract.buildRootUri(AUTHORITY_STORAGE, "primary");
        intentGetContent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, hintUri);

        testDialogs = new TestDialogController();
    }

    @Test
    public void testOnDocumentPicked() {
        DocumentInfo doc = new DocumentInfo();
        doc.userId = TestProvidersAccess.USER_ID;
        doc.authority = "authority";
        doc.documentId = "documentId";

        PickActivity pickActivity = mRule.launchActivity(intentGetContent);
        pickActivity.mState.canShareAcrossProfile = true;
        pickActivity.onDocumentPicked(doc);
        SystemClock.sleep(3000);

        Instrumentation.ActivityResult result = mRule.getActivityResult();
        assertThat(pickActivity.isFinishing()).isTrue();
        assertThat(result.getResultCode()).isEqualTo(Activity.RESULT_OK);
        assertThat(result.getResultData().getData()).isEqualTo(doc.getDocumentUri());
    }

    @Test
    public void testOnDocumentPicked_otherUser() {
        DocumentInfo doc = new DocumentInfo();
        doc.userId = TestProvidersAccess.OtherUser.USER_ID;
        doc.authority = "authority";
        doc.documentId = "documentId";

        PickActivity pickActivity = mRule.launchActivity(intentGetContent);
        pickActivity.mState.canShareAcrossProfile = true;
        pickActivity.onDocumentPicked(doc);
        SystemClock.sleep(3000);

        Instrumentation.ActivityResult result = mRule.getActivityResult();
        assertThat(result.getResultCode()).isEqualTo(Activity.RESULT_OK);
        assertThat(result.getResultData().getData()).isEqualTo(doc.getDocumentUri());
    }

    @Test
    public void testOnDocumentPicked_otherUserDoesNotReturn() {
        DocumentInfo doc = new DocumentInfo();
        doc.userId = TestProvidersAccess.OtherUser.USER_ID;
        doc.authority = "authority";
        doc.documentId = "documentId";

        PickActivity pickActivity = mRule.launchActivity(intentGetContent);
        pickActivity.mState.canShareAcrossProfile = false;
        pickActivity.getInjector().dialogs = testDialogs;
        pickActivity.onDocumentPicked(doc);
        SystemClock.sleep(3000);

        assertThat(pickActivity.isFinishing()).isFalse();
        testDialogs.assertActionNotAllowedShown();
    }

    @Test
    public void testStartForResultForwarderActivity() {
        Intent originalIntent = new Intent("com.android.documentsui.test.action.RETURN_RESULT");
        Intent intent = ForResultForwarderActivity.getIntent(mTargetContext, originalIntent,
                TestProvidersAccess.USER_ID);

        PickActivity pickActivity = mRule.launchActivity(intentGetContent);
        pickActivity.startActivityForResult(intent, AbstractActionHandler.CODE_FORWARD);
        SystemClock.sleep(3000);

        Instrumentation.ActivityResult result = mRule.getActivityResult();
        assertThat(result.getResultCode()).isEqualTo(Activity.RESULT_OK);
        assertThat(result.getResultData().getStringExtra(RESULT_EXTRA)).isEqualTo(RESULT_DATA);
    }

    @Test
    public void testStartForResultForwarderActivity_noActivity() {
        Intent originalIntent = new Intent("no_app_handles_this_intent_action");
        Intent intent = ForResultForwarderActivity.getIntent(mTargetContext, originalIntent,
                TestProvidersAccess.USER_ID);

        PickActivity pickActivity = mRule.launchActivity(intentGetContent);
        pickActivity.startActivityForResult(intent, AbstractActionHandler.CODE_FORWARD);
        SystemClock.sleep(3000);

        assertThat(pickActivity.isFinishing()).isFalse();
    }

    public static class ReturnResultActivity extends Activity {
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Intent data = new Intent();
            data.putExtra(RESULT_EXTRA, RESULT_DATA);
            setResult(Activity.RESULT_OK, data);
            finish();
        }
    }
}