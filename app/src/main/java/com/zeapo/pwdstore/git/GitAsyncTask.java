package com.zeapo.pwdstore.git;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;

import com.zeapo.pwdstore.PasswordStore;
import com.zeapo.pwdstore.R;
import com.zeapo.pwdstore.utils.PasswordRepository;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.GitCommand;


public class GitAsyncTask extends AsyncTask<GitCommand, Integer, String> {
    private Activity activity;
    private boolean finishOnEnd;
    private boolean refreshListOnEnd;
    private ProgressDialog dialog;
    private Class operation;

    public GitAsyncTask(Activity activity, boolean finishOnEnd, boolean refreshListOnEnd, Class operation) {
        this.activity = activity;
        this.finishOnEnd = finishOnEnd;
        this.refreshListOnEnd = refreshListOnEnd;
        this.operation = operation;

        dialog = new ProgressDialog(this.activity);
    }

    protected void onPreExecute() {
        this.dialog.setMessage(activity.getResources().getString(R.string.running_dialog_text));
        this.dialog.setCancelable(false);
        this.dialog.show();
    }

    @Override
    protected String doInBackground(GitCommand... cmd) {
        for (GitCommand aCmd : cmd) {
            try {
                aCmd.call();
            } catch (Exception e) {
                e.printStackTrace();
                return e.getMessage();
            }
        }
        return "";
    }

    protected void onPostExecute(String result) {
        if (this.dialog != null)
            try {
                this.dialog.dismiss();
            } catch (Exception e)
            {
                // ignore
            }

        if (result == null)
            result = "Unexpected error";

        if (!result.isEmpty()) {
            new AlertDialog.Builder(activity).
                    setTitle(activity.getResources().getString(R.string.jgit_error_dialog_title)).
                    setMessage(activity.getResources().getString(R.string.jgit_error_dialog_text) + result).
                    setPositiveButton(activity.getResources().getString(R.string.dialog_ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if (operation.equals(CloneCommand.class)) {
                                // if we were unable to finish the job
                                try {
                                    FileUtils.deleteDirectory(PasswordRepository.getWorkTree());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            } else {
                                activity.setResult(Activity.RESULT_CANCELED);
                                activity.finish();
                            }
                        }
                    }).show();

        } else {
            if (finishOnEnd) {
                this.activity.setResult(Activity.RESULT_OK);
                this.activity.finish();
            }

            if (refreshListOnEnd) {
                try {
                    ((PasswordStore) this.activity).updateListAdapter();
                } catch (ClassCastException e) {
                    // oups, mistake
                }
            }
        }
    }
}
