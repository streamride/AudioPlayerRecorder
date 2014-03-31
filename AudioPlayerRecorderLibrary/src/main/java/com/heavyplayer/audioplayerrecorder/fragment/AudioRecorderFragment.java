package com.heavyplayer.audioplayerrecorder.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;
import com.heavyplayer.audioplayerrecorder.R;
import com.heavyplayer.audioplayerrecorder.service.AudioRecorderService;
import com.heavyplayer.audioplayerrecorder.widget.AudioRecorderMicrophone;

public class AudioRecorderFragment extends DialogFragment implements DialogInterface.OnClickListener {
	public static final String TAG = AudioRecorderFragment.class.getSimpleName();

	private static final String ARG_FILE_URI = "arg_file_uri";

	private AudioRecorderService.LocalBinder mAudioRecorderBinder;
	private ServiceConnection mServiceConnection = new AudioRecorderServiceConnection();

	private Uri mFileUri;

	public static AudioRecorderFragment newInstance(Uri fileUri) {
		return newInstance(new AudioRecorderFragment(), fileUri);
	}
	protected static <T extends AudioRecorderFragment> T newInstance(T fragment, Uri fileUri) {
		final Bundle args = new Bundle();
		args.putParcelable(ARG_FILE_URI, fileUri);

		fragment.setArguments(args);

		return fragment;
	}

	public static AudioRecorderFragment createInstance(Uri fileUri) {
		final Bundle args = new Bundle();
		args.putParcelable(ARG_FILE_URI, fileUri);

		final AudioRecorderFragment fragment = new AudioRecorderFragment();
		fragment.setArguments(args);

		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		if(savedInstanceState == null)
			startService();

		super.onCreate(savedInstanceState);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		bindService();
	}

	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final Activity activity = getActivity();

		final View view = LayoutInflater.from(activity).inflate(R.layout.audio_recorder, null);

		return new AlertDialog.Builder(activity)
				.setTitle(R.string.audio_recorder_recording)
				.setView(view)
				.setPositiveButton(R.string.audio_recorder_stop_recording, this)
				.create();
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		unbindServiceAndStop();
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		super.onCancel(dialog);
		unbindServiceAndStop();
	}

	@Override
	public void onPause() {
		super.onPause();
		unbindService();
	}

	protected void startService() {
		final Activity activity = getActivity();
		activity.startService(AudioRecorderService.getLaunchIntent(activity, getFileUri()));
	}

	protected void bindService() {
		// Establish a connection with the service.  We use an explicit
		// class name because we want a specific service implementation that
		// we know will be running in our own process (and thus won't be
		// supporting component replacement by other applications).
		getActivity().bindService(
				new Intent(getActivity(), AudioRecorderService.class),
				mServiceConnection,
				Context.BIND_AUTO_CREATE);
	}

	protected void unbindService() {
		if(mAudioRecorderBinder != null) {
			unregisterMicrophone();

			// Detach our existing connection.
			getActivity().unbindService(mServiceConnection);
			mAudioRecorderBinder = null;
		}
	}

	protected void unbindServiceAndStop() {
		unbindService();
		getActivity().stopService(new Intent(getActivity(), AudioRecorderService.class));
		onStopRecording();
	}

	protected void registerMicrophone() {
		final AudioRecorderMicrophone microphone =
				(AudioRecorderMicrophone)getDialog().findViewById(android.R.id.input);
		if(mAudioRecorderBinder != null && microphone != null)
			mAudioRecorderBinder.registerAudioRecorderMicrophone(microphone);
	}

	protected void unregisterMicrophone() {
		final AudioRecorderMicrophone microphone =
				(AudioRecorderMicrophone)getDialog().findViewById(android.R.id.input);
		if(mAudioRecorderBinder != null && microphone != null)
			mAudioRecorderBinder.unregisterAudioRecorderMicrophone(microphone);
	}

	public void onStopRecording() {
		// Purposely empty.
	}

	public Uri getFileUri() {
		if(mFileUri == null)
			mFileUri = getArguments().getParcelable(ARG_FILE_URI);
		return mFileUri;
	}

	private class AudioRecorderServiceConnection implements ServiceConnection {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			// This is called when the connection with the service has been
			// established, giving us the service object we can use to
			// interact with the service.  Because we have bound to a explicit
			// service that we know is running in our own process, we can
			// cast its IBinder to a concrete class and directly access it.

			// Configure microphone media recorder. Used to update sound amplitude animation.
			mAudioRecorderBinder = (AudioRecorderService.LocalBinder)service;
			registerMicrophone();

			// Tell the user about this for our demo.
			Toast.makeText(getActivity(), R.string.local_service_connected, Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			// Because it is running in our same process, we should never
			// see this happen.
			mAudioRecorderBinder = null;

			onStopRecording();
			dismiss();

			Toast.makeText(getActivity(), R.string.local_service_disconnected, Toast.LENGTH_SHORT).show();
		}
	}
}
