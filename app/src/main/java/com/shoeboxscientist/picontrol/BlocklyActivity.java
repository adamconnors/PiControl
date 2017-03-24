package com.shoeboxscientist.picontrol;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.webkit.WebView;
import android.widget.Toast;

import com.google.blockly.android.AbstractBlocklyActivity;
import com.google.blockly.android.codegen.CodeGenerationRequest;

import java.util.Arrays;
import java.util.List;

public class BlocklyActivity extends AbstractBlocklyActivity {
    private static final String TAG = "PiControl.Blockly";

    private PiControlApplication.NetworkCallbackInterface mNetworkCallbacks = new NetworkCallbacks();
    private WebViewManager mWebView;
    private RobotCommandExecutor mCommandListener;

    // Developer tools link for command block.
    //https://blockly-demo.appspot.com/static/demos/blockfactory/index.html#822i3z
    private static final List<String> BLOCK_DEFINITIONS = Arrays.asList(
            "custom/command_blocks.json",
            "default/list_blocks.json",
            "default/logic_blocks.json",
            "default/loop_blocks.json",
            "default/math_blocks.json",
            "default/text_blocks.json",
            "default/variable_blocks.json",
            "default/colour_blocks.json"
    );
    private static final List<String> JAVASCRIPT_GENERATORS = Arrays.asList(
            // Custom block generators go here. Default blocks are already included.
            "custom/command_generator.js"
    );

    MyCodeGenerationCallback mCodeGeneratorCallback =
            new MyCodeGenerationCallback();

    @Override
    protected View onCreateContentView(int containerId) {
        View root = getLayoutInflater().inflate(R.layout.activity_blockly, null);
        mWebView = new WebViewManager(this, (WebView) root.findViewById(R.id.webview));
        mWebView.setWebViewEventListener(new WebViewManager.WebViewEventListener() {
            @Override
            public void onWebViewInitialised() {
                mWebView.loadStream(PiControlApplication.getInstance().getStreamUrl());
            }
        });
        mWebView.init();
        return root;
    }

    @Override
    protected void onStop() {

        // Save the workspace before we leave. Also generate the code so we can run it from
        // the main activity.
        onSaveWorkspace();
        mCodeGeneratorCallback.setGenerateOnly(true);
        onRunCode();
        super.onStop();
    }

    @Override
    protected void onRunCode() {
        onSaveWorkspace();
        super.onRunCode();
    }

    @NonNull
    @Override
    protected List<String> getBlockDefinitionsJsonPaths() {
        return BLOCK_DEFINITIONS;
    }

    @NonNull
    @Override
    protected String getToolboxContentsXmlPath() {
        return "custom/toolbox.xml";
    }

    @NonNull
    @Override
    protected List<String> getGeneratorsJsPaths() {
        return JAVASCRIPT_GENERATORS;
    }

    @NonNull
    @Override
    protected CodeGenerationRequest.CodeGeneratorCallback getCodeGenerationCallback() {
        // Uses the same callback for every generation call.
        return mCodeGeneratorCallback;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCommandListener = new RobotCommandExecutor(new RobotCommandExecutor.ExecutionListener() {
            @Override
            public void print(String msg) {
                Toast.makeText(BlocklyActivity.this, msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        PiControlApplication.getInstance().setNetworkCallbackActivity(mNetworkCallbacks);
        mWebView.setRobotEventListener(mCommandListener);
        onLoadWorkspace();
    }

    @Override
    protected void onInitBlankWorkspace() {
        // Initialize variable names.
        // TODO: (#22) Remove this override when variables are supported properly
        getController().requestDeleteVariable("item");
        getController().addVariable("millis");
        getController().addVariable("counter");
    }

    private class MyCodeGenerationCallback implements CodeGenerationRequest.CodeGeneratorCallback {

        // Flag that will generate but not run the code. This is so that I can easily
        // make a save event available to the main activity.
        private boolean generateOnly = false;

        public void setGenerateOnly(boolean generateOnly) {
            this.generateOnly = generateOnly;
        }

        public boolean isGenerateOnly() {
            return generateOnly;
        }

        @Override
        public void onFinishCodeGeneration(String generatedCode) {

            if (generatedCode.isEmpty()) {
                Toast.makeText(BlocklyActivity.this,
                        "Something went wrong with code generation.", Toast.LENGTH_LONG).show();
            } else {
                PiControlApplication.getInstance().setLastRunScript(generatedCode);

                if (!generateOnly) {
                    mWebView.executeMacroScript(generatedCode);
                }
            }

            // This flag is set when the code is saved and unset so that all run events
            // really do run the code.
            generateOnly = false;
        }


    }

    // Will receive callbacks to network events here.
    private class NetworkCallbacks implements PiControlApplication.NetworkCallbackInterface {

        @Override
        public void pingResult(boolean success) {

        }

        @Override
        public void hotspotResult(boolean success, String msg) {

        }

        @Override
        public void cmdResult(boolean success) {

        }
    }
}
