package com.studi.timesyncwifi.Dialogs;


import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.studi.timesyncwifi.Activity.ClientActivity;
import com.studi.timesyncwifi.Activity.ServerActivity;
import com.studi.timesyncwifi.Activity.TimeSyncActivity;
import com.studi.timesyncwifi.R;

public class SelectPositionDialog extends DialogFragment {

    private final String TAG = "TimeSync:Dialog";

    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        View v = inflater.inflate(R.layout.select_place_dialog, null);
        builder.setView(v);


        Button hog2Button = (Button) v.findViewById(R.id.hog2Button);
        Button hog1Button = (Button) v.findViewById(R.id.hog1Button);
        Button backButton = (Button) v.findViewById(R.id.backButton);

        hog1Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Integer.parseInt(Build.VERSION.RELEASE.split("\\.")[0])>=5) {
                    Intent intent = new Intent(getActivity(), ServerActivity.class);
                    getActivity().startActivity(intent);
                    if (!(getActivity() instanceof TimeSyncActivity)) {
                        getActivity().finish();
                    }
                } else {
                    Toast.makeText(getContext(), "To old Android Version to be Server", Toast.LENGTH_LONG).show();
                }
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), ClientActivity.class);
                getActivity().startActivity(intent);
                if (!(getActivity() instanceof TimeSyncActivity)) {
                    getActivity().finish();
                }
            }
        });



        return builder.create();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        if ((getActivity() instanceof TimeSyncActivity)) {
            DialogFragment dialog1 = new SelectPositionDialog();
            dialog1.show(getActivity().getSupportFragmentManager(), "SelectPositionDialog");
        }
    }
}








