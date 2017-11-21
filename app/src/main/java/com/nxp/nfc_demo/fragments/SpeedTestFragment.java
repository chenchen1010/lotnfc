/*
****************************************************************************
* Copyright(c) 2014 NXP Semiconductors                                     *
* All rights are reserved.                                                 *
*                                                                          *
* Software that is described herein is for illustrative purposes only.     *
* This software is supplied "AS IS" without any warranties of any kind,    *
* and NXP Semiconductors disclaims any and all warranties, express or      *
* implied, including all implied warranties of merchantability,            *
* fitness for a particular purpose and non-infringement of intellectual    *
* property rights.  NXP Semiconductors assumes no responsibility           *
* or liability for the use of the software, conveys no license or          *
* rights under any patent, copyright, mask work right, or any other        *
* intellectual property rights in or to any products. NXP Semiconductors   *
* reserves the right to make changes in the software without notification. *
* NXP Semiconductors also makes no representation or warranty that such    *
* application will be suitable for the specified use without further       *
* testing or modification.                                                 *
*                                                                          *
* Permission to use, copy, modify, and distribute this software and its    *
* documentation is hereby granted, under NXP Semiconductors' relevant      *
* copyrights in the software, without fee, provided that it is used in     *
* conjunction with NXP Semiconductor products(UCODE I2C, NTAG I2C).        *
* This  copyright, permission, and disclaimer notice must appear in all    *
* copies of this code.                                                     *
****************************************************************************
*/
package com.nxp.nfc_demo.fragments;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;

import com.nxp.nfc_demo.activities.MainActivity;
import com.nxp.ntagi2cdemo.R;

public class SpeedTestFragment extends Fragment implements
		OnCheckedChangeListener, OnClickListener {

	private static RadioGroup rf_readOptions;
	private static RadioGroup rf_memOptions;
	private static TextView rf_textCallback;
	private static TextView rf_datarateCallback;
	private static boolean rf_chosen = false;
	private static boolean rf_MemChosen = false;
	private static EditText rf_EditCharMulti;
	private static TextView rf_TextCharMulti;
	private static Button rf_ButtonSpeedtest;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Values per default
		rf_MemChosen = true;
		rf_chosen = true;

		setRetainInstance(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View layout = inflater.inflate(R.layout.fragment_speedtest, container,
				false);

		rf_ButtonSpeedtest = (Button) layout.findViewById(R.id.startSpeedtest);
		rf_ButtonSpeedtest.setOnClickListener(this);

		rf_readOptions = (RadioGroup) layout
				.findViewById(R.id.radioReadOptions);

		rf_memOptions = (RadioGroup) layout
				.findViewById(R.id.radioMemoryOptions);
		rf_memOptions.setOnCheckedChangeListener(this);

		rf_textCallback = (TextView) layout.findViewById(R.id.rf_textCallback);
		rf_datarateCallback = (TextView) layout.findViewById(R.id.rf_datarateCallback);
		rf_datarateCallback.setMovementMethod(new ScrollingMovementMethod());

		rf_EditCharMulti = (EditText) layout.findViewById(R.id.editCharMultipl);
		rf_TextCharMulti = (TextView) layout.findViewById(R.id.textCharMultipl);

		rf_EditCharMulti.setText("10");
		rf_EditCharMulti.addTextChangedListener(charMultiListener);

		return layout;
	}
	
	private final TextWatcher charMultiListener = new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        public void afterTextChanged(Editable s) {
            if (rf_MemChosen == false) {
            	try
                {	
            		int bytes = Integer.parseInt(rf_EditCharMulti.getText().toString());
            		int overhead = eepromCalculateOverhead(bytes);

                	if(overhead > 0)
        				rf_TextCharMulti.setText("+ " + overhead + " " + getActivity().getResources().getString(R.string.Block_multipl_eeprom_overhead));
        			else
        				rf_TextCharMulti.setText(getActivity().getResources().getString(R.string.Block_multipl_eeprom));
                } catch (NumberFormatException ex) {
                	rf_TextCharMulti.setText(getActivity().getResources().getString(R.string.Block_multipl_eeprom));
                	ex.printStackTrace();
                }
            }
        }
    };
	
	@Override
	public void onCheckedChanged(RadioGroup group, int checkedId) {
		if (checkedId == R.id.radioMemoryEeprom) {
			rf_MemChosen = false;
			rf_readOptions.setVisibility(View.GONE);
			rf_EditCharMulti.setHint(R.string.Ndef_char_multipl);
			
			String textCharMulti = getrf_ndef_value_charmulti();
			
			// getting text multiplier
			int chMultiplier = 1;
			int chMultiLength = textCharMulti.length();
			if (chMultiLength == 0) {
				chMultiplier = 1;
			} else {
				chMultiplier = Integer.parseInt(textCharMulti);
			}

			rf_EditCharMulti.setText(Integer.toString(chMultiplier * 64));
			// rf_ndef_CharMulti.setText("");
		} else if (checkedId == R.id.radioMemorySram) {
			rf_MemChosen = true;
//			rf_readOptions.setVisibility(View.VISIBLE);
			rf_readOptions.setVisibility(View.GONE);
			rf_EditCharMulti.setHint(R.string.Block_multipl);
			rf_TextCharMulti.setText(getActivity().getResources().getString(R.string.Block_multipl_sram));
			
			String textCharMulti = getrf_ndef_value_charmulti();

			// getting text multiplier
			int chMultiplier = 1;
			int chMultiLength = textCharMulti.length();
			if (chMultiLength == 0) {
				chMultiplier = 1;
			} else {
				chMultiplier = Integer.parseInt(textCharMulti);
			}

			rf_EditCharMulti.setText(Integer.toString(chMultiplier / 64));
		}
	}
	
	private int eepromCalculateOverhead(int bytes) {
		int overhead = 0;
				
		String messageText = "";
		for (int i = 0; i < bytes; i++) {
			messageText = messageText.concat(" ");
		}
		
		// Calculate the overhead
		NdefMessage msg;
		try {
			msg = createNdefMessage(messageText);
			
			int ndef_message_size = (msg.toByteArray().length + 5);
			ndef_message_size = (int) Math.round(ndef_message_size / 4)	* 4;
			
			overhead = ndef_message_size - (bytes);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			e.printStackTrace();
		}
		
		return overhead;
	}
	
	private NdefMessage createNdefMessage(String text)
			throws UnsupportedEncodingException {
		String lang = "en";
		byte[] textBytes = text.getBytes();
		byte[] langBytes = lang.getBytes("US-ASCII");
		int langLength = langBytes.length;
		int textLength = textBytes.length;
		byte[] payload = new byte[1 + langLength + textLength];
		payload[0] = (byte) langLength;
		System.arraycopy(langBytes, 0, payload, 1, langLength);
		System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength);

		NdefRecord record = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
				NdefRecord.RTD_TEXT, new byte[0], payload);

		NdefRecord[] records = { record };
		NdefMessage message = new NdefMessage(records);

		return message;
	}

	private void StartEEPROMSpeedTest() {
		if (MainActivity.demo.isReady() && MainActivity.demo.isConnected()) {
			MainActivity.demo.FinishAllTasks();
			try {
				MainActivity.demo.EEPROMSpeedtest();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (FormatException e) {
				e.printStackTrace();
			}
		}
	}

	private void StartSRAMSpeedTest() {
		if (MainActivity.demo.isReady() && MainActivity.demo.isConnected()) {
			MainActivity.demo.FinishAllTasks();
			try {
				MainActivity.demo.SRAMSpeedtest();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (FormatException e) {
				e.printStackTrace();
			}
		}
	}

	public static String getrf_ndef_value_charmulti() {
		return rf_EditCharMulti.getText().toString();
	}

	public static Boolean isSRamEnabled() {
		return rf_MemChosen;
	}

	public static void setAnswer(String answer) {
		rf_textCallback.setText(answer);

		// Reset datarate textview
		rf_datarateCallback.setText("");
	}

	public static boolean getChosen() {
		return rf_chosen;
	}

	public static String getReadOptions() {
		int id = rf_readOptions.getCheckedRadioButtonId();
		View radioButton = rf_readOptions.findViewById(id);
		int radioId = rf_readOptions.indexOfChild(radioButton);
		RadioButton btn = (RadioButton) rf_readOptions.getChildAt(radioId);
		return (String) btn.getText();
	}

	public static void setReadOptions(int i) {
		rf_readOptions.check(i);
	}

	public static void setDatarateCallback(String datarate) {
		rf_datarateCallback.setText(datarate);
	}

	public static String getDatarateCallback() {
		return rf_datarateCallback.getText().toString();
	}

	@Override
	public void onClick(View v) {
		if (rf_MemChosen)
			StartSRAMSpeedTest();
		else
			StartEEPROMSpeedTest();

	}

}
