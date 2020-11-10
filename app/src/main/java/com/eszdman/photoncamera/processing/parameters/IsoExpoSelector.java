package com.eszdman.photoncamera.processing.parameters;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.util.Log;
import android.util.Range;

import com.eszdman.photoncamera.api.CameraMode;
import com.eszdman.photoncamera.app.PhotonCamera;
import com.eszdman.photoncamera.ui.camera.CameraFragment;

public class IsoExpoSelector {
    public static final int baseFrame = 1;
    private static final String TAG = "IsoExpoSelector";
    public static boolean HDR = false;
    public static boolean useTripod = false;


    public static void setExpo(CaptureRequest.Builder builder, int step) {
        Log.v(TAG, "InputParams: " +
                "expo time:" + ExposureIndex.sec2string(ExposureIndex.time2sec(PhotonCamera.getCameraFragment().mPreviewExposureTime)) +
                " iso:" + PhotonCamera.getCameraFragment().mPreviewIso+ " analog:"+getISOAnalog());
        ExpoPair pair = GenerateExpoPair(step);
        Log.v(TAG, "IsoSelected:" + pair.iso +
                " ExpoSelected:" + ExposureIndex.sec2string(ExposureIndex.time2sec(pair.exposure)) + " sec step:" + step + " HDR:" + HDR);

        builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
        builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, pair.exposure);
        builder.set(CaptureRequest.SENSOR_SENSITIVITY, (int)pair.iso);
    }

    public static ExpoPair GenerateExpoPair(int step) {
        double mpy = 1.0;
        ExpoPair pair = new ExpoPair(CameraFragment.context.mPreviewExposureTime, getEXPLOW(), getEXPHIGH(),
                CameraFragment.context.mPreviewIso, getISOLOW(), getISOHIGH(),getISOAnalog());
        pair.normalizeiso100();
        if (PhotonCamera.getSettings().selectedMode == CameraMode.NIGHT)
            mpy = mpy*1.5;
        if (pair.exposure < ExposureIndex.sec / 40 && pair.normalizedIso() > 90.0/4000.0) {
            pair.ReduceIso();
        }
        if (pair.exposure < ExposureIndex.sec / 13 && pair.normalizedIso() > 750.0/4000.0) {
            pair.ReduceIso();
        }
        if (pair.exposure < ExposureIndex.sec / 8 && pair.normalizedIso() > 1500.0/4000.0) {
            if (step != baseFrame || !PhotonCamera.getSettings().eisPhoto) pair.ReduceIso();
        }
        if (pair.exposure < ExposureIndex.sec / 8 && pair.normalizedIso() > 1500.0/4000.0) {
            if (step != baseFrame || !PhotonCamera.getSettings().eisPhoto) pair.ReduceIso(1.25);
        }
        if (pair.normalizedIso() >= 12700.0/4000.0) {
            pair.ReduceIso();
        }
        if (CameraFragment.mTargetFormat == CameraFragment.rawFormat) {
            if (pair.iso >= (100.0/4000.0) / 0.65) pair.iso *= mpy;
            else {
                pair.exposure *= mpy;
            }
        }
        if (useTripod) {
            pair.MinIso();
        }

        double currentManExp = PhotonCamera.getManualMode().getCurrentExposureValue();
        double currentManISO = PhotonCamera.getManualMode().getCurrentISOValue();
        pair.exposure = currentManExp != 0 ? (long) currentManExp : pair.exposure;
        pair.iso = currentManISO != 0 ? (int) currentManISO : pair.iso;

        if (step == 3 && HDR) {
            pair.ExpoCompensateLower(1.0 / 1.0);
        }
        if (step == 2 && HDR) {
            pair.ExpoCompensateLower(8.0);
        }
        if (pair.exposure < ExposureIndex.sec / 90 && PhotonCamera.getSettings().eisPhoto) {
            //HDR = true;
        }
        if (step == baseFrame) {
            if (pair.normalizedIso() <= 120.0/4400.0 && pair.exposure > ExposureIndex.sec / 70.0/4400.0 && PhotonCamera.getSettings().eisPhoto) {
                pair.ReduceExpo();
            }
            if (pair.normalizedIso() <= 245.0/4400.0 && pair.exposure > ExposureIndex.sec / 50.0/4400.0 && PhotonCamera.getSettings().eisPhoto) {
                pair.ReduceExpo();
            }
            if (pair.exposure < ExposureIndex.sec * 3.00 && pair.exposure > ExposureIndex.sec / 3 && pair.normalizedIso() < 3200.0/4400.0 && PhotonCamera.getSettings().eisPhoto) {
                pair.FixedExpo(1.0 / 8);
                if (pair.normalizeCheck())
                    PhotonCamera.getCameraFragment().showToast("Wrong parameters: iso:" + pair.iso + " exp:" + pair.exposure);
            }
        }
        pair.denormalizeSystem();
        return pair;
    }

    public static double getMPY() {
        return 50.0 / getISOLOW();
    }

    private static int mpyIso(int in) {
        return (int) (in * getMPY());
    }

    private static int getISOHIGH() {
        Object key = CameraFragment.mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);
        if (key == null) return 3200;
        else {
            return (int) ((Range) (key)).getUpper();
        }
    }

    public static int getISOHIGHExt() {
        return mpyIso(getISOHIGH());
    }

    private static int getISOLOW() {
        Object key = CameraFragment.mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);
        if (key == null) return 100;
        else {
            return (int) ((Range) (key)).getLower();
        }
    }
    public static int getISOAnalog() {
        Object key = CameraFragment.mCameraCharacteristics.get(CameraCharacteristics.SENSOR_MAX_ANALOG_SENSITIVITY);
        if (key == null) return 100;
        else {
            return (int)(key);
        }
    }

    public static int getISOLOWExt() {
        return mpyIso(getISOLOW());
    }

    public static long getEXPHIGH() {
        Object key = CameraFragment.mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE);
        if (key == null) return ExposureIndex.sec;
        else {
            return (long) ((Range) (key)).getUpper();
        }
    }

    public static long getEXPLOW() {
        Object key = CameraFragment.mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE);
        if (key == null) return ExposureIndex.sec / 1000;
        else {
            return (long) ((Range) (key)).getLower();
        }
    }


    //==================================Class : ExpoPair==================================//

    public static class ExpoPair {
        public long exposure;
        public int iso;
        long exposurehigh, exposurelow;
        int isolow, isohigh,isoanalog;

        public ExpoPair(ExpoPair pair) {
            copyfrom(pair);
        }

        public ExpoPair(long expo, long expl, long exph, int is, int islow, int ishigh, int analog) {
            exposure = expo;
            iso = is;
            exposurehigh = exph;
            exposurelow = expl;
            isolow = islow;
            isohigh = ishigh;
            isoanalog = analog;
        }

        public void copyfrom(ExpoPair pair) {
            exposure = pair.exposure;
            exposurelow = pair.exposurelow;
            exposurehigh = pair.exposurehigh;
            iso = pair.iso;
            isolow = pair.isolow;
            isohigh = pair.isohigh;
            isoanalog = pair.isoanalog;
        }

        public void normalizeiso100() {
            double mpy = 100.0 / isolow;
            iso *= mpy;
        }

        public void denormalizeSystem() {
            double div = 100.0 / isolow;
            iso /= div;
        }
        public float normalizedIso(){
            return (float)iso/isoanalog;
        }
        public void normalize() {
            double div = 100.0 / isolow;
            if (iso / div > isohigh) iso = isohigh;
            if (iso / div < isolow) iso = isolow;
            if (exposure > exposurehigh) exposure = exposurehigh;
            if (exposure < exposurelow) exposure = exposurelow;
        }

        public boolean normalizeCheck() {
            double div = 100.0 / isolow;
            boolean wrongparams = false;
            if (iso / div > isohigh) wrongparams = true;
            if (iso / div < isolow) wrongparams = true;
            if (exposure > exposurehigh) wrongparams = true;
            if (exposure < exposurelow) wrongparams = true;
            return wrongparams;
        }

        public void ExpoCompensateLower(double k) {
            iso /= k;
            if (normalizeCheck()) {
                iso *= k;
                exposure /= k;
                if (normalizeCheck()) {
                    exposure *= k;
                }
            }
        }

        public void MinIso() {
            double k = iso / 101.0;
            ReduceIso(k);
            if (normalizeCheck()) {
                iso *= (double) (exposure) / exposurehigh;
                exposure = exposurehigh;
                if (normalizeCheck()) {
                    iso = isohigh;
                }
            }
        }

        public void ReduceIso() {
            ReduceIso(2.0);
            if (normalizeCheck()) {
                ReduceIso(1.0 / 2);
            }
        }

        public void ReduceIso(double k) {
            iso /= k;
            exposure *= k;
        }

        public void ReduceExpo() {
            ReduceExpo(2.0);
            if (normalizeCheck()) ReduceExpo(1.0 / 2);
        }

        public void ReduceExpo(double k) {
            Log.d(TAG, "ExpoReducing iso:" + iso + " expo:" + ExposureIndex.sec2string(ExposureIndex.time2sec(exposure)));
            iso *= k;
            exposure /= k;
            Log.d(TAG, "ExpoReducing done iso:" + iso + " expo:" + ExposureIndex.sec2string(ExposureIndex.time2sec(exposure)));
        }

        public void FixedExpo(double expo) {
            long expol = ExposureIndex.sec2time(expo);
            double k = (double) exposure / expol;
            ReduceExpo(k);
            Log.d(TAG, "ExpoFixating iso:" + iso + " expo:" + ExposureIndex.sec2string(ExposureIndex.time2sec(exposure)));
            if (normalizeCheck()) ReduceExpo(1 / k);
        }

        public String ExposureString() {
            return ExposureIndex.sec2string(ExposureIndex.time2sec(exposure));
        }
    }
}
