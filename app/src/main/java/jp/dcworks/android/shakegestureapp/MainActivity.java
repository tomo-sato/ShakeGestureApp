package jp.dcworks.android.shakegestureapp;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import jp.dcworks.android.shakegestureapp.util.arrayFilter.ArrayFilter;
import jp.dcworks.android.shakegestureapp.util.arrayFilter.LaplacianArrayFilter;
import jp.dcworks.android.shakegestureapp.util.dimension.DimensionExchanger;
import jp.dcworks.android.shakegestureapp.util.dimension.ScalerDimensionExchanger;
import jp.dcworks.android.shakegestureapp.util.dpMatching.DPMatching;
import jp.dcworks.android.shakegestureapp.util.interpolation.LinearInterpolation;
import jp.dcworks.android.shakegestureapp.util.model.Acceleration3dPoint;
import jp.dcworks.android.shakegestureapp.util.model.MatchingArray;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    /** ログ出力用　タグ文字列 */
    private static final String LOG_TAG = MainActivity.class.getName();

    /** SDカードファイルpath センサーデータの保存先 */
    private static final String SD_FILE_PATH = Environment.getExternalStorageDirectory() + "/jp.dcworks.android.shakegestureapp/" ;

    /** 開始タイマーの遅延時間3秒 */
    private static final int TIMER_START_DELAY = 3000;
    /** 終了タイマーの遅延時間（センサー取得時間）5秒 */
    private static final int TIMER_END_DELAY = 5000;

    /** 実行中のスレッドカウント */
    private static int THREAD_ALIVE_COUNT = 0;

    /** センサー取得値 X軸 */
    private ArrayList<Double> mSensorValuesX = null;
    /** センサー取得値 Y軸 */
    private ArrayList<Double> mSensorValuesY = null;
    /** センサー取得値 Z軸 */
    private ArrayList<Double> mSensorValuesZ = null;

    /** ログ取得用 */
    private StringBuffer mLogBuf = null;

    /* (非 Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 保存データ取得
        SharedPreferences pref = getSharedPreferences(Constant.PREFERENCES_KEY, Activity.MODE_PRIVATE);

        // プリファレンスから取得か、デフォルト値をセット
        EditText editPatternName1 = (EditText) findViewById(R.id.editPatternName1);
        editPatternName1.setText(pref.getString(String.valueOf(R.id.editPatternName1), getResources().getString(R.string.edit_default_pattern_name1)));

        EditText editPatternName2 = (EditText) findViewById(R.id.editPatternName2);
        editPatternName2.setText(pref.getString(String.valueOf(R.id.editPatternName2), getResources().getString(R.string.edit_default_pattern_name2)));

        EditText editPatternName3 = (EditText) findViewById(R.id.editPatternName3);
        editPatternName3.setText(pref.getString(String.valueOf(R.id.editPatternName3), getResources().getString(R.string.edit_default_pattern_name3)));
    }

    /* (非 Javadoc)
     * @see android.hardware.SensorEventListener#onAccuracyChanged(android.hardware.Sensor, int)
     */
    @Override
    public void onAccuracyChanged(Sensor arg0, int arg1) {
        // センサーイベントハンドラ（センサーの精度が変更された時）
        // 今回未使用
    }

    /* (非 Javadoc)
     * @see android.hardware.SensorEventListener#onSensorChanged(android.hardware.SensorEvent)
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        // 取得したセンサ－情報をメモリに保持
        try{
            mSensorValuesX.add((double)event.values[0]);
            mSensorValuesY.add((double)event.values[1]);
            mSensorValuesZ.add((double)event.values[2]);
        }catch (NullPointerException e) {
            // Androidのメモリ管理により、メンバ変数がnullとなる可能性がある為。
            e.printStackTrace();
        }
    }

    /**
     * センサー処理開始時のタイマータスク
     * @param viewId
     * @param textId
     * @param fileName
     * @param isSave
     * @return
     */
    private TimerTask getTimerTasc(final int viewId,
                                   final int textId,
                                   final String fileName,
                                   final boolean isSave){

        // TimerTask内で必要なパラメータ初期化
        final Handler handler = new Handler();
        final SensorEventListener sensorEventListener = this;
        final Context context = this;

        return new TimerTask() {
            @Override
            public void run() {
                // ボリューム設定
                AudioManager manager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
                int vol = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                manager.setStreamVolume(AudioManager.STREAM_MUSIC, vol/2, 0);

                // センサー処理開始のアラームを鳴らす。
                MediaPlayer mp = MediaPlayer.create(context, R.raw.beep_start);
                mp.start();

                //センサーマネージャの取得
                SensorManager sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
                // 加速度センサーを指定してSensorインスタンスを取得
                Sensor accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                if (accelerometerSensor != null) {
                    //リスナー登録
                    sensorManager.registerListener(
                            sensorEventListener,
                            accelerometerSensor,
                            SensorManager.SENSOR_DELAY_GAME);
                }

                // プログレスダイアログ表示処理・センサー終了処理をハンドラに委託。
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        // プログレスダイアログを表示
                        final ProgressDialog progressDialog = new ProgressDialog(context);
                        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                        progressDialog.setMessage(getString(R.string.progress_gesture_message_body));
                        progressDialog.setCancelable(false);
                        progressDialog.show();

                        // 初回処理の遅延用にタイマースレッドの生成
                        Timer endTimer = new Timer(false);
                        // スケジュールを設定
                        endTimer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                // センサー処理終了のアラームを鳴らす。
                                MediaPlayer mp = MediaPlayer.create(context, R.raw.beep_end);
                                mp.start();

                                // センサーリスナーを解除
                                SensorManager sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
                                sensorManager.unregisterListener(sensorEventListener);

                                // ログを取得する。
                                get3PointsLog(viewId, fileName, isSave);

                                // プログレスダイアログ終了
                                progressDialog.dismiss();

                                // 引数により、保存・認証の切り替え制御
                                if(isSave){
                                    // 取得したセンサーデータを保存
                                    if(save(viewId, fileName)){
                                        // 正常に終了したら、プリファレンスに名称を保存。
                                        // パターン名称をプリファレンスに保持
                                        SharedPreferences pref = getSharedPreferences(Constant.PREFERENCES_KEY, Activity.MODE_PRIVATE);
                                        SharedPreferences.Editor editor = pref.edit();
                                        editor.putString(String.valueOf(textId), fileName);
                                        editor.commit();
                                    }
                                }else{
                                    // 認証処理
                                    auth(handler);
                                }
                            }
                        }, TIMER_END_DELAY); // 遅延時間を指定し、処理を終了。
                    }
                });
            }
        };
    }

    /**
     * パターン記録ボタンクリックイベント
     * @param view
     */
    public void onClickPatternSaveBtn(final View view){

        // 押されたボタンに対するテキストを取得する。
        String strPatternName = Constant.BLANK;
        EditText editPatternName = null;
        int editTextId = 0;
        switch (view.getId()) {
            case R.id.buttonPatternSave1:
                editPatternName = (EditText) findViewById(R.id.editPatternName1);
                strPatternName = ((SpannableStringBuilder)editPatternName.getText()).toString();
                editTextId = R.id.editPatternName1;
                break;

            case R.id.buttonPatternSave2:
                editPatternName = (EditText) findViewById(R.id.editPatternName2);
                strPatternName = ((SpannableStringBuilder)editPatternName.getText()).toString();
                editTextId = R.id.editPatternName2;
                break;

            case R.id.buttonPatternSave3:
                editPatternName = (EditText) findViewById(R.id.editPatternName3);
                strPatternName = ((SpannableStringBuilder)editPatternName.getText()).toString();
                editTextId = R.id.editPatternName3;
                break;

            default:
                break;
        }

        // 必須チェック　パターン名
        if(Constant.BLANK.equals(strPatternName)){
            // Toastのインスタンスを生成
            Toast toast = Toast.makeText(this, getString(R.string.toast_pattern_name_required), Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            // 未入力はエラー
            return;
        }

        // ダイアログで必要なパラメータ初期化
        final String fileName = strPatternName;
        final int textId = editTextId;

        // ダイアログ表示
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_pattern_message_title, strPatternName))
                .setMessage(getString(R.string.dialog_pattern_message_body, TIMER_START_DELAY / 1000, TIMER_END_DELAY / 1000))
                .setPositiveButton(getString(R.string.dialog_button_start), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        // メモリ保持用メンバ変数初期化
                        mSensorValuesX = new ArrayList<Double>();
                        mSensorValuesY = new ArrayList<Double>();
                        mSensorValuesZ = new ArrayList<Double>();

                        // 初回処理の遅延用にタイマー、スレッドの生成
                        Timer startTimer = new Timer(false);
                        // スケジュールを設定：遅延時間を指定し、処理を開始。
                        startTimer.schedule(getTimerTasc(view.getId(), textId, fileName, true), TIMER_START_DELAY);
                    }
                })
                .setNegativeButton(getString(R.string.dialog_button_cancel), null)
                .show();
    }

    /**
     * 認証ボタンクリックイベント
     * @param view
     */
    public void onClickPatternAuthBtn(View view){
        // ダイアログ表示
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_auth_message_title))
                .setMessage(getString(R.string.dialog_auth_message_body, TIMER_START_DELAY / 1000, TIMER_END_DELAY / 1000))
                .setPositiveButton(getString(R.string.dialog_button_start), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // メモリ保持用メンバ変数初期化
                        mSensorValuesX = new ArrayList<Double>();
                        mSensorValuesY = new ArrayList<Double>();
                        mSensorValuesZ = new ArrayList<Double>();

                        // 初回処理の遅延用にタイマー、スレッドの生成
                        Timer startTimer = new Timer(false);
                        // スケジュールを設定：遅延時間を指定し、処理を開始。
                        startTimer.schedule(getTimerTasc(0, 0, Constant.BLANK, false), TIMER_START_DELAY);
                    }
                })
                .setNegativeButton(getString(R.string.dialog_button_cancel), null)
                .show();
    }

    /**
     * センサー認証メソッド
     */
    public void auth(final Handler handler){
        final Context context = this;
        handler.post(new Runnable() {
            @Override
            public void run() {
                // ぐるぐる表示
                ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressbar);
                progressBar.setVisibility(View.VISIBLE);
            }
        });

        // 保存データ取得
        String tmpPatternName = Constant.BLANK;
        HashMap<Integer, String> patternNames = new HashMap<Integer, String>();
        SharedPreferences pref = getSharedPreferences(Constant.PREFERENCES_KEY, Activity.MODE_PRIVATE);

        tmpPatternName = pref.getString(String.valueOf(R.id.editPatternName1), "");
        if(!Constant.BLANK.equals(tmpPatternName)) patternNames.put(R.id.buttonPatternSave1, tmpPatternName);

        tmpPatternName = pref.getString(String.valueOf(R.id.editPatternName2), "");
        if(!Constant.BLANK.equals(tmpPatternName)) patternNames.put(R.id.buttonPatternSave2, tmpPatternName);

        tmpPatternName = pref.getString(String.valueOf(R.id.editPatternName3), "");
        if(!Constant.BLANK.equals(tmpPatternName)) patternNames.put(R.id.buttonPatternSave3, tmpPatternName);

        // 認証時のジェスチャーを取得
        final Double[] matchingArray1 = getMatchingArray(mSensorValuesX.toArray(new Double[]{}),
                mSensorValuesY.toArray(new Double[]{}),
                mSensorValuesZ.toArray(new Double[]{}));

        THREAD_ALIVE_COUNT = patternNames.size();
        if(THREAD_ALIVE_COUNT <= 0){
            // 名称が0件の場合エラー
            handler.post(new Runnable() {
                @Override
                public void run() {
                    // ぐるぐる表示
                    ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressbar);
                    progressBar.setVisibility(View.INVISIBLE);

                    // Toastのインスタンスを生成
                    Toast toast = Toast.makeText(context, getString(R.string.toast_auth_error_message), Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                }
            });
        }

        mLogBuf = new StringBuffer();
        // ファイルからJSON配列を取得する。
        for(Iterator<Map.Entry<Integer, String>> it = patternNames.entrySet().iterator(); it.hasNext();){

            Map.Entry<Integer, String> entry = it.next();
            final String patternName = entry.getValue();
            File file = new File(SD_FILE_PATH + patternName + "_" + entry.getKey() + ".json");

            // ファイルが存在したらマッチング処理を開始。
            if(file.exists()){
                final String json = fileToString(file);
                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        JSONObject jsonObject = null;
                        try {
                            Log.d(LOG_TAG, "test1");
                            try {
                                jsonObject = new JSONObject(json);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            JSONArray jsonXArray = (JSONArray)jsonObject.get("X");
                            JSONArray jsonYArray = (JSONArray)jsonObject.get("Y");
                            JSONArray jsonZArray = (JSONArray)jsonObject.get("Z");

                            Double[] xPoints = new Double[jsonXArray.length()];
                            Double[] yPoints = new Double[jsonYArray.length()];
                            Double[] zPoints = new Double[jsonZArray.length()];

                            for(int j = 0; j < jsonXArray.length(); j++){
                                xPoints[j] = new Double(jsonXArray.getDouble(j));
                                yPoints[j] = new Double(jsonYArray.getDouble(j));
                                zPoints[j] = new Double(jsonZArray.getDouble(j));
                            }

                            Log.d(LOG_TAG, "test2");
                            Double[] matchingArray2 = getMatchingArray(xPoints, yPoints, zPoints);

                            Log.d(LOG_TAG, "test3");
                            MatchingArray ma = new MatchingArray(matchingArray1, matchingArray2);

                            Log.d(LOG_TAG, "test4");
                            DPMatching m = new DPMatching(ma, new LinearInterpolation());
                            final Double score = m.matching();

                            Log.d(LOG_TAG, patternName + " : " + score);
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    // TextView生成
                                    TextView textViewScore = new TextView(context);
                                    String str = patternName + " : " + String.valueOf(score);
                                    textViewScore.setText(str);
                                    mLogBuf.append(str).append("\n");

                                    // スコア表示部LinearLayoutを取得する。
                                    LinearLayout linearLayoutScore = (LinearLayout)findViewById(R.id.linearLayoutScore);
                                    linearLayoutScore.addView(textViewScore);

                                    THREAD_ALIVE_COUNT--;
                                    // コールバック関数
                                    callbackAuth(handler);
                                }
                            });
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
                t.start();

                // ファイルが存在しなかったらマッチング処理は行わず、カウントのみをデクリメントする。
            }else{
                THREAD_ALIVE_COUNT--;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        // コールバック関数
                        // 記録ファイルを手動で全部消してしまった場合且つ、プリファレンスに名称が存在する場合。
                        // プロトなので空ファイル生成。
                        callbackAuth(handler);
                    }
                });
            }
        }
    }

    /**
     * 終了通知を取得する。
     * @param handler
     */
    private void callbackAuth(final Handler handler) {
        final Context context = this;
        if(THREAD_ALIVE_COUNT <= 0){
            // ぐるぐる表示
            ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressbar);
            progressBar.setVisibility(View.INVISIBLE);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    // プレフィックスを生成
                    Date date = new Date();
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd.hhmmssSSS");
                    String dateStr = dateFormat.format(date);

                    // SDカード保存
                    String filePath = SD_FILE_PATH + "/log/" + dateStr + ".shake_log";
                    File file = new File(filePath);
                    file.getParentFile().mkdir();
                    FileOutputStream fos;
                    try {
                        fos = new FileOutputStream(file, false);
                        OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
                        BufferedWriter bw = new BufferedWriter(osw);
                        bw.write(mLogBuf.toString());
                        bw.flush();
                        bw.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    // Toastのインスタンスを生成
                    Toast toast = Toast.makeText(context, getString(R.string.toast_finish_auth_message), Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                }
            });
        }
    }

    /**
     * センサー保存メソッド
     * 押されたviewのID、パターン名称をファイル名にjsonファイルを生成。
     * SDカードに保持する。
     * @param viewId
     * @param fileName
     */
    public boolean save(int viewId, String fileName){
        // 新規生成
        JSONObject rootObject = new JSONObject();

        JSONArray childArrayXObject = new JSONArray();
        JSONArray childArrayYObject = new JSONArray();
        JSONArray childArrayZObject = new JSONArray();
        String jsonStr = "";
        try {
            for(int i = 0;i < mSensorValuesX.size(); i++){
                childArrayXObject.put(mSensorValuesX.get(i));
                childArrayYObject.put(mSensorValuesY.get(i));
                childArrayZObject.put(mSensorValuesZ.get(i));
            }
            rootObject.put("X", childArrayXObject);
            rootObject.put("Y", childArrayYObject);
            rootObject.put("Z", childArrayZObject);

            jsonStr = rootObject.toString(2);
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        // SDカード保存
        String filePath = SD_FILE_PATH + fileName + "_" + viewId + ".json";
        File file = new File(filePath);
        file.getParentFile().mkdir();
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(file, false);
            OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
            BufferedWriter bw = new BufferedWriter(osw);
            bw.write(jsonStr);
            bw.flush();
            bw.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /* (非 Javadoc)
     * @see android.app.Activity#onPause()
     */
    @Override
    protected void onPause() {
        super.onPause();
        // センサーリスナーを解除
        SensorManager sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        sensorManager.unregisterListener(this);
    }

    /**
     * ファイルから文字列を取得
     * @param file
     * @return
     */
    public static String fileToString(File file) {
        BufferedReader br = null;
        String retStr = "";
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            StringBuffer sb = new StringBuffer();
            int c;
            while ((c = br.read()) != -1) {
                sb.append((char) c);
            }
            retStr = sb.toString();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // クローズできなかったら何もしない。
            try {br.close();} catch (Exception e) {};
        }
        return retStr;
    }

    /**
     * マッチング用の配列を取得する。
     * @param xPoints
     * @param yPoints
     * @param zPoints
     * @return
     */
    public Double[] getMatchingArray(Double[] xPoints, Double[] yPoints, Double[] zPoints){
        // 引数チェック
        if((xPoints == null || yPoints == null || zPoints == null)){
            String message = new StringBuffer("xPoints=").append(xPoints)
                    .append(", yPoints=").append(yPoints)
                    .append(", zPoints=").append(zPoints)
                    .toString();
            throw new IllegalArgumentException(message);
        }
        if((xPoints.length != yPoints.length)
                || (xPoints.length != zPoints.length)
                || (yPoints.length != zPoints.length)){
            String message = new StringBuffer("xPoints.length=").append(xPoints.length)
                    .append(", yPoints.length=").append(yPoints.length)
                    .append(", zPoints.length=").append(zPoints.length)
                    .toString();
            throw new IllegalArgumentException(message);
        }

        // 1次元配列変換用に使用するオブジェクトに変換
        ArrayList<Acceleration3dPoint> acceleration3dPoints = new ArrayList<Acceleration3dPoint>();
        for(int i = 0; i < xPoints.length; i++){
            acceleration3dPoints.add(new Acceleration3dPoint(xPoints[i], yPoints[i], zPoints[i]));
        }

        // 3次元座標を1次元配列に直す
        DimensionExchanger dimensionExchanger = new ScalerDimensionExchanger();
        double[] dimensions = dimensionExchanger.exchange(acceleration3dPoints.toArray(new Acceleration3dPoint[]{}));
        Double[] tmpDimensions = new Double[dimensions.length];
        for(int i = 0; i < dimensions.length; i++){
            tmpDimensions[i] = new Double(dimensions[i]);
        }

        // 勾配フィルタをかける
        ArrayFilter filter = new LaplacianArrayFilter();
        return (Double[])filter.convert(tmpDimensions);
    }

    /**
     * ログを取得する。
     * 	Excelで加工しやすいよう、カンマ区切りで保持。
     * 	ファイルが時系列で並ぶようにプレフィックスに時間を使用。
     * @param viewId
     * @param fileName
     * @param isSave
     */
    private void get3PointsLog(int viewId, String fileName, boolean isSave) {
        // プレフィックスを生成
        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd.hhmmssSSS");
        String dateStr = dateFormat.format(date);

        if(!isSave){
            fileName = dateStr + ".auth_log";
        }else{
            fileName = dateStr + "." + fileName + ".save_log";
        }

        StringBuffer xBuf = new StringBuffer("X");
        StringBuffer yBuf = new StringBuffer("Y");
        StringBuffer zBuf = new StringBuffer("Z");
        try{
            for(int i = 0; i < mSensorValuesX.size(); i++){
                xBuf.append(",").append(mSensorValuesX.get(i));
                yBuf.append(",").append(mSensorValuesY.get(i));
                zBuf.append(",").append(mSensorValuesZ.get(i));
            }
            File file = new File(SD_FILE_PATH + "/log/" + fileName);
            file.getParentFile().mkdir();
            FileOutputStream fos = new FileOutputStream(file, false);
            OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
            BufferedWriter bw = new BufferedWriter(osw);

            bw.write(xBuf.toString());
            bw.newLine();
            bw.write(yBuf.toString());
            bw.newLine();
            bw.write(zBuf.toString());

            bw.flush();
            bw.close();
        }catch (Exception e) {
            // Androidのメモリ管理により、メンバ変数がnullとなる可能性がある為。
            e.printStackTrace();
        }
    }
}
