package com.example.myheadacheapp

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import android.app.DatePickerDialog
import android.widget.DatePicker
import android.widget.EditText
import java.util.Calendar
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import android.util.Log

class MainActivity : AppCompatActivity() {

    // ログレベルを BODY に設定し、すべてのリクエスト/レスポンスを出力
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    // OkHttpClient をカスタマイズして作成
    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    private lateinit var editTextDate: EditText
    private lateinit var editTextTime: EditText
    private lateinit var spinnerLocation: Spinner
    private lateinit var spinnerSeverity: Spinner
    private lateinit var checkBoxAura: CheckBox
    private lateinit var checkBoxTookMedicine: CheckBox
    private lateinit var editTextMedicineName: EditText
    private lateinit var spinnerAfterSeverity: Spinner
    private lateinit var buttonRecord: Button

    // OkHttpClient を使い回す場合はメンバ変数で保持
    // private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. ViewをIDで取得
        editTextDate = findViewById(R.id.editTextDate)
        editTextTime = findViewById(R.id.editTextTime)
        spinnerLocation = findViewById(R.id.spinnerLocation)
        spinnerSeverity = findViewById(R.id.spinnerSeverity)
        checkBoxAura = findViewById(R.id.checkBoxAura)
        checkBoxTookMedicine = findViewById(R.id.checkBoxTookMedicine)
        editTextMedicineName = findViewById(R.id.editTextMedicineName)
        spinnerAfterSeverity = findViewById(R.id.spinnerAfterSeverity)
        buttonRecord = findViewById(R.id.buttonRecord)

        // 起動時に現在日時をセット
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        // Set Date
        val dateString = "%04d/%02d/%02d".format(year, month + 1, day)
        editTextDate.setText(dateString)

        // Set Time
        val timeString = "%02d:%02d".format(hour, minute)
        editTextTime.setText(timeString)


        // 日付タップでDatePickerを表示
        editTextDate.setOnClickListener {
            showDatePickerDialog()
        }

        // 時間タップでTimePickerを表示
        editTextTime.setOnClickListener {
            showTimePickerDialog()
        }

        // デフォルトの薬名をセット
        checkBoxTookMedicine.isChecked = true
        editTextMedicineName.setText(R.string.default_medicine_name)

        // 2. Spinnerにアダプタを設定
        setupSpinner(spinnerLocation, R.array.location_array)
        setupSpinner(spinnerSeverity, R.array.severity_array)
        setupSpinner(spinnerAfterSeverity, R.array.severity_array)

        // 3. 薬服用チェックボックスの状態で、薬名入力欄の有効/無効を切り替え
        checkBoxTookMedicine.setOnCheckedChangeListener { _, isChecked ->
            editTextMedicineName.isEnabled = isChecked
            if (!isChecked) {
                // OFFになったら空文字に
                editTextMedicineName.setText("")
            } else {
                // ONになったらデフォルト薬名に
                editTextMedicineName.setText(R.string.default_medicine_name)
            }
        }

        // 4. 記録ボタン押下
        buttonRecord.setOnClickListener {
            // UIからデータ取得
            val date = editTextDate.text.toString()
            val time = editTextTime.text.toString()
            val location = spinnerLocation.selectedItem.toString()
            val severity = spinnerSeverity.selectedItem.toString()
            val hasAura = checkBoxAura.isChecked
            val tookMedicine = checkBoxTookMedicine.isChecked
            val medicineName = editTextMedicineName.text.toString()
            val afterSeverity = spinnerAfterSeverity.selectedItem.toString()

            // 入力チェック(例として、日付と時間が未入力ならエラー表示)
            if (date.isBlank() || time.isBlank()) {
                Toast.makeText(this, "日付・時間を入力してください", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // JSONオブジェクトを作成 (Apps Script 側で受け取るキーに合わせる)
            val json = JSONObject().apply {
                put("date", date)
                put("time", time)
                put("location", location)
                put("severity", severity)
                put("hasAura", hasAura)
                put("tookMedicine", tookMedicine)
                put("medicineName", medicineName)
                put("afterSeverity", afterSeverity)
            }

            // スプレッドシートAPIへPOST (OkHttpで非同期)
            postDataToSpreadSheet(json)
        }
    }

    /**
     * Spinnerに配列リソースを紐付ける
     */
    private fun setupSpinner(spinner: Spinner, arrayResId: Int) {
        val adapter = ArrayAdapter.createFromResource(
            this,
            arrayResId,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
    }

    /**
     * スプレッドシートのAPIにデータをPOST
     */
    private fun postDataToSpreadSheet(jsonData: JSONObject) {
        // 実際にはGoogle Apps Script のウェブアプリURLを指定する (例: https://script.google.com/macros/s/.../exec)
        // ここでは例として example.com/exec
        val url = BuildConfig.ENDPOINT_URL
        Log.d("API_DEBUG", "Sending JSON: ${jsonData.toString()}")

        // JSONをRequestBodyに変換
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val requestBody: RequestBody = jsonData.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        // 非同期通信
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // 通信エラー時
                runOnUiThread {
                    /* Toast.makeText(
                        this@MainActivity,
                        "通信に失敗しました: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show() */

                    // Logcat にも詳細を出す
                    Log.e("API_DEBUG", "onFailure - message: ${e.message}", e)
                    showFailureDialog()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                // レスポンス取得
                response.use {
                    if (!it.isSuccessful) {
                        // ステータスコードがエラーの場合
                        val bodyText = it.body?.string() ?: ""
                        runOnUiThread {
                            /* Toast.makeText(
                                this@MainActivity,
                                "サーバーエラー: ${response.code}",
                                Toast.LENGTH_LONG
                            ).show() */

                            // ログ出力
                            Log.e("API_DEBUG", "Error code: ${response.code}, body: $bodyText")
                            showFailureDialog()
                        }
                    } else {
                        // 正常に受け取れた
                        val responseBody = it.body?.string() ?: ""
                        runOnUiThread {
                            // ここでレスポンス(JSON)をパースして成功メッセージを表示する例
                            // (Apps Scriptが {status: "success"} を返す想定)
                            /* Toast.makeText(
                                this@MainActivity,
                                "記録しました: $responseBody",
                                Toast.LENGTH_LONG
                            ).show() */
                            Log.d("API_DEBUG", "Success response: $responseBody")
                            showSuccessDialog()
                        }
                    }
                }
            }
        })
    }


    private fun showDatePickerDialog() {
        // 現在の日付を初期値として設定
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _: DatePicker, selectedYear: Int, selectedMonth: Int, selectedDay: Int ->
                // "YYYY/MM/DD" の形で EditText にセット
                // ※ selectedMonth は 0～11 のため +1 する
                val dateString = "%04d/%02d/%02d".format(selectedYear, selectedMonth + 1, selectedDay)
                editTextDate.setText(dateString)
            },
            year, month, day
        )
        datePickerDialog.show()
    }

    private fun showTimePickerDialog() {
        // 現在の時刻を初期値として設定
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        // TimePickerDialog で24時間制 or 12時間制を指定（trueで24時間制）
        val is24HourView = true

        val timePickerDialog = android.app.TimePickerDialog(
            this,
            { _, selectedHour: Int, selectedMinute: Int ->
                // "HH:mm" 形式で表示
                val timeString = "%02d:%02d".format(selectedHour, selectedMinute)
                editTextTime.setText(timeString)
            },
            hour, minute, is24HourView
        )
        timePickerDialog.show()
    }

    @SuppressLint("SetTextI18n")
    private fun resetInputFields() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        // 日付・時刻を現在に戻す
        editTextDate.setText("%04d/%02d/%02d".format(year, month + 1, day))
        editTextTime.setText("%02d:%02d".format(hour, minute))

        // 薬服用チェックを ON にして、薬名をデフォルトに
        checkBoxTookMedicine.isChecked = true
        editTextMedicineName.setText(R.string.default_medicine_name)

        // 閃輝暗点のCheckBoxは OFF に戻す (例)
        checkBoxAura.isChecked = false

        // Spinner を先頭アイテムに設定 (index=0)
        spinnerLocation.setSelection(0)
        spinnerSeverity.setSelection(0)
        spinnerAfterSeverity.setSelection(0)
    }

    private fun showSuccessDialog() {
        // 例: responseBody にサーバから返るメッセージが入っているなら、それを使うことも可能
        val dialogBuilder = android.app.AlertDialog.Builder(this)
        dialogBuilder.setTitle("記録完了")
        dialogBuilder.setMessage("スプレッドシートに記録しました。")
        dialogBuilder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
            // 入力を初期化
            resetInputFields()
        }
        dialogBuilder.show()
    }

    private fun showFailureDialog() {
        val dialogBuilder = android.app.AlertDialog.Builder(this)
        dialogBuilder.setTitle("記録失敗")
        dialogBuilder.setMessage("スプレッドシートへの記録に失敗しました。")
        dialogBuilder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
            // 入力欄はそのまま
        }
        dialogBuilder.show()
    }


}
