package ru.miacomsoft.androidsignalserverbarnaul.Lib;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;


/// <uses-permission android:name="android.permission.RECORD_AUDIO" />
/***
 *      Контроллер распознования / синтеза речи
 *
 *         public VoiseCtrl vois;
 *
 *         vois = new VoiseCtrl(this);
 *         vois.checkVoiceCommandPermission(this);
 *         vois.start();
 *         vois.recognizerText((String msg) -> {
 *             Log.d("TEST", "partial_results: " + msg);
 *             vois.send("вы сказали. " + msg);
 *         });
 */
public class VoiseCtrl implements TextToSpeech.OnInitListener {

    public interface CallbackMethod {
        public void call(String msg);
    }


    private Context context;
    private TextToSpeech tts;
    private SpeechRecognizer speech = null;
    private Intent recognizerIntent;
    private AudioManager myAudioManager;
    private HashMap<String, String> params = new HashMap<String, String>();
    private ArrayList<String> voiceMessageQueueArray = new ArrayList<String>();
    private ArrayList<CallbackMethod> callbackMethod = new ArrayList<CallbackMethod>();

    public VoiseCtrl(Context contect) {
        this.context = contect;
        TextSpeechProgressListener textSpeech = new TextSpeechProgressListener(); //Экземпляр класса слушателя
        tts = new TextToSpeech(this.context, this);
        tts.setOnUtteranceProgressListener(textSpeech); //Установка слушателя синтеза речи
        // Создание экземпляра класса AudioManager (необходимо для управление громкостью динамика)
        myAudioManager = (AudioManager) this.context.getSystemService(Context.AUDIO_SERVICE);
    }


    /**
     * Поставить  голосовое сообщение в очередь, или проговорить его
     *
     * @param text
     */
    public void send(String text) {
        voiceMessageQueueArray.add(text);
    }


    public void send(String text, CallbackMethod m) {
        callbackMethod.add(m);
        voiceMessageQueueArray.add(text);
    }


    public void recognizerText(CallbackMethod m) {
        callbackMethod.add(m);
    }


    /**
     * Обработчик  распозногого текста
     *
     * @param text
     */
    public void getRecognizerText(String text) {

    }

    /**
     * Обработчик списка распозногого текста
     *
     * @param textArr
     */
    public void getRecognizerArrayText(ArrayList<String> textArr) {

    }

    /**
     * Обработчик ошибок при распозновании
     *
     * @param text
     */
    public void getRecognizerError(String text) {

    }

    /**
     * Проговорить очередь сообщений из накопленого списка
     */
    public void speakTheQueueOfMessagesFromTheList() {
        if (voiceMessageQueueArray.size() > 0) {
            speak_on(); // Включаем динамики
            params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "HELLO");
            for (int i = 0; i < voiceMessageQueueArray.size(); i++) {
                tts.speak(voiceMessageQueueArray.get(i), TextToSpeech.QUEUE_ADD, params);// Синтезировать речь
            }
            voiceMessageQueueArray.clear();
        }
    }

    /**
     * Запуск распознования  текста
     */
    public void start() {
        speak_off();
        turnOnTheListenerVoices(); // Вызов метода для активизации распознавателя голоса
        speech.startListening(recognizerIntent); // Начать прослушивание речи
    }

    /***
     * Отключение распознованияттекста
     */
    public void stop() {
        if (speech != null) {
            speech.stopListening(); //Прекратить слушать речь
            speech.destroy();       // Уничтожить объект SpeechRecognizer
        }
    }

    /**
     * Запуск окна разрешения для включения доступа к микурофону
     *
     * @param context
     */
    public void checkVoiceCommandPermission(Context context) {
        // Включить разрешение  прослушивать микрофон в настройках
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)) {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + context.getPackageName()));
                context.startActivity(intent);
            }
        }
    }

    /**
     * Функция уничтожения приложения
     */
    public void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }

    /**
     * Выключение звука
     */
    public void speak_off() { // Метод для выключение внешних динамиков планшета
        myAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
    }

    /**
     * Включение звука
     */
    public void speak_on() { // Метод включения внешних динамиков планшета
        myAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 20, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
    }

    /**
     * Инициализация синтеза речи
     *
     * @param status
     */
    @Override
    public void onInit(int status) {   // Инициализация перед синтезом речи
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.getDefault());
            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "This Language is not supported");
            }
        } else {
            Log.e("TTS", "Init Failed!");
        }

    }

    /**
     * Запуск очередного цикла распознования текста из голоса
     */
    public void turnOnTheListenerVoices() {
        speech = SpeechRecognizer.createSpeechRecognizer(context); //Создание объекта распознавателя речи
        speech.setRecognitionListener(recognitionListener); //Установить обработчик событий распознавания
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.getPackageName());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, 1); // Включить Офлайновое распознование
    }

    /**
     * Контроллер для распознования голоса
     */
    RecognitionListener recognitionListener = new RecognitionListener() {
        @Override
        public void onBeginningOfSpeech() {
        }

        @Override
        public void onBufferReceived(byte[] buffer) {
        }

        @Override
        public void onEndOfSpeech() {
            // oText.setText("НЕ ГОВОРИ");
        }

        @Override
        public void onError(int errorCode) {
            speak_off();  //Выключить звук в случае любой ошибки
            String errorMessage = getErrorText(errorCode); // Вызов метода расшифровки ошибки
            getRecognizerError(errorMessage + "; Error=#" + errorCode);
            speakTheQueueOfMessagesFromTheList(); // проговорить очередь сообщений из списка
            speech.destroy();
            turnOnTheListenerVoices();
            speech.startListening(recognizerIntent);
        }

        @Override
        public void onEvent(int arg0, Bundle arg1) {

        }

        @Override
        public void onPartialResults(Bundle arg0) {
            ArrayList data = arg0.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            // String word = (String) data.get(data.size() - 1);
            String sp = data.get(0).toString();
            // returnedText.setText(sp);
            // Log.i("TEST", "partial_results: " + sp);
        }

        @Override
        public void onReadyForSpeech(Bundle arg0) {

        }

        @Override
        public void onResults(Bundle results) {  // Результаты распознавания
            ArrayList data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            String sp = data.get(0).toString();
            getRecognizerText(sp);
            ArrayList<String> resArray = new ArrayList<String>();
            for (int i = 0; i < data.size(); i++) {
                resArray.add(data.get(i).toString());
            }
            getRecognizerArrayText(resArray); // Обработка массива результатов
            // Log.i("TEST", "partial_results: " + sp);
            speakTheQueueOfMessagesFromTheList(); // проговорить очередь сообщений из списка
            for (int i = 0; i < callbackMethod.size(); i++) {
                speak_on();
                callbackMethod.get(i).call(sp);
            }
            speak_off(); // Если фразы и команды не описаны, выполняется распознавание речи, вывод результата
            // в виде строки при выключенных динамиках
            speech.stopListening(); //Прекратить слушать речь
            speech.destroy();       // Уничтожить объект SpeechRecognizer
            turnOnTheListenerVoices();
            speech.startListening(recognizerIntent);
        }

        @Override
        public void onRmsChanged(float rmsdB) {

        }

    };

    /**
     * Слушатель голоса
     */
    public class TextSpeechProgressListener extends UtteranceProgressListener {
        @Override
        public void onDone(String utteranceId) { // Действия после окончания речи синтезатором
            ((Activity) context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    /*
                    r19 = sp.compareTo(sp19);
                    if (r19 != 0) { // Если не "конец связи", то активити распознавания голоса запускается вновь
                        oText.setText("Слушаю");
                        speech.startListening(recognizerIntent);
                    }
                    */
                    speech.startListening(recognizerIntent);
                }

            });
        }

        @Override
        public void onStart(String utteranceId) {
        }

        @Override
        public void onError(String utteranceId) {
        }
    }

    /**
     * Расшифровка ошибок при распозновании тектса
     *
     * @param errorCode
     * @return
     */
    public static String getErrorText(int errorCode) {
        String message;
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                message = "Audio recording error";
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                message = "Client side error";
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                message = "Insufficient permissions";
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                message = "Network error";
                break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                message = "Network timeout";
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                message = "No match";
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                message = "RecognitionService busy";
                break;
            case SpeechRecognizer.ERROR_SERVER:
                message = "error from server";
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                message = "No speech input";
                break;
            default:
                message = "Didn't understand, please try again.";
                break;
        }
        return message;
    }


}
