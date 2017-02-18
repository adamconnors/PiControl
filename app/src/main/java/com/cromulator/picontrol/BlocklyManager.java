package com.cromulator.picontrol;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

//import com.google.blockly.model.Block;

/**
 * Wraps all the interactions with Blockly editing and execution.
 */
public class BlocklyManager {

    private View mStartEditor;
    private View mRunScript;
    private Window mRoot;
    private RobotCommandExecutor mCommandExecutor;

    public BlocklyManager(final Context ctx, Window root, final WebViewManager webview) {
        mRoot = root;
        mStartEditor = root.findViewById(R.id.edit_script);
        mCommandExecutor = new RobotCommandExecutor(null);
        webview.setRobotEventListener(mCommandExecutor);
        mStartEditor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ctx.startActivity(new Intent(ctx, BlocklyActivity.class));
            }
        });

        mRunScript = root.findViewById(R.id.run_script);
        mRunScript.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String s = PiControlApplication.getInstance().getLastRunScript();
                if (s == null) {
                    Toast.makeText(ctx, "No script to run.", Toast.LENGTH_SHORT).show();
                } else {
                    webview.executeMacroScript(s);
                }
            }
        });
    }
}
