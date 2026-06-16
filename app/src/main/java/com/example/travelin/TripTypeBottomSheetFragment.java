package com.example.travelin;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

public class TripTypeBottomSheetFragment extends BottomSheetDialogFragment {
    private static final String TYPE_FUTURE = "FUTURE";
    private static final String TYPE_CURRENT = "CURRENT";
    private static final String TYPE_PAST = "PAST";

    private LinearLayout futureOption;
    private LinearLayout currentOption;
    private LinearLayout pastOption;
    private RadioButton futureRadio;
    private RadioButton currentRadio;
    private RadioButton pastRadio;
    private MaterialButton continueButton;
    private String selectedTripType;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_trip_type_bottom_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        futureOption = view.findViewById(R.id.option_future);
        currentOption = view.findViewById(R.id.option_current);
        pastOption = view.findViewById(R.id.option_past);
        futureRadio = view.findViewById(R.id.radio_future);
        currentRadio = view.findViewById(R.id.radio_current);
        pastRadio = view.findViewById(R.id.radio_past);
        continueButton = view.findViewById(R.id.btn_continue);

        ImageButton closeButton = view.findViewById(R.id.btn_close);
        MaterialButton cancelButton = view.findViewById(R.id.btn_cancel);

        closeButton.setOnClickListener(v -> dismiss());
        cancelButton.setOnClickListener(v -> dismiss());
        futureOption.setOnClickListener(v -> selectTripType(TYPE_FUTURE));
        currentOption.setOnClickListener(v -> selectTripType(TYPE_CURRENT));
        pastOption.setOnClickListener(v -> selectTripType(TYPE_PAST));

        continueButton.setEnabled(false);
        continueButton.setOnClickListener(v -> {
            if (selectedTripType == null) {
                return;
            }

            Intent intent = new Intent(requireContext(), AddTripActivity.class);
            intent.putExtra("trip_type", selectedTripType);
            startActivity(intent);
            dismiss();
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() instanceof BottomSheetDialog) {
            BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
            View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.setBackground(new ColorDrawable(Color.TRANSPARENT));
            }
        }
    }

    private void selectTripType(String tripType) {
        selectedTripType = tripType;

        boolean isFuture = TYPE_FUTURE.equals(tripType);
        boolean isCurrent = TYPE_CURRENT.equals(tripType);
        boolean isPast = TYPE_PAST.equals(tripType);

        futureRadio.setChecked(isFuture);
        currentRadio.setChecked(isCurrent);
        pastRadio.setChecked(isPast);

        futureOption.setBackgroundResource(isFuture ? R.drawable.trip_type_option_selected_background : R.drawable.trip_type_option_background);
        currentOption.setBackgroundResource(isCurrent ? R.drawable.trip_type_option_selected_background : R.drawable.trip_type_option_background);
        pastOption.setBackgroundResource(isPast ? R.drawable.trip_type_option_selected_background : R.drawable.trip_type_option_background);

        int primary = Color.parseColor("#007A8C");
        continueButton.setEnabled(true);
        continueButton.setBackgroundTintList(ColorStateList.valueOf(primary));
        continueButton.setTextColor(Color.WHITE);
    }
}
