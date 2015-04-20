package com.getataxi.client.comm.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import com.getataxi.client.R;
import com.getataxi.client.comm.models.LocationDM;
import com.getataxi.client.utils.LocationListViewAdapter;
import com.getataxi.client.utils.UserPreferencesManager;

import java.util.List;

/**
 * Created by bvb on 19.4.2015 г..
 */
public class SelectLocationDialogFragment extends DialogFragment {
    public interface SelectLocationDialogListener {
        public void onDialogNegativeClick(DialogFragment dialog);
        public void onLocationSelect(DialogFragment dialog, LocationDM locationDM);
    }

    // Use this instance of the interface to deliver action events
    SelectLocationDialogListener mListener;

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (SelectLocationDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement SelectLocationDialogListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.select_location);
        final List<LocationDM> locations = UserPreferencesManager.loadLocations(getActivity());
        if(locations != null) {
        final LocationListViewAdapter adapter = new LocationListViewAdapter(getActivity(), R.layout.location_list_item ,locations);

            builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    LocationDM selectedLocation = locations.get(which);
                    mListener.onLocationSelect(SelectLocationDialogFragment.this, selectedLocation);
                }
            });
        }
        builder.setNegativeButton(R.string.cancel_selection,new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mListener.onDialogNegativeClick(SelectLocationDialogFragment.this);
            }
        });
        return builder.create();
    }
}
