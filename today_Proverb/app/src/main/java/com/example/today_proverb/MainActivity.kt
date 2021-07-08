package com.example.today_proverb

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.absoluteValue

class MainActivity : AppCompatActivity() {

    private val viewPager: ViewPager2 by lazy {
        findViewById(R.id.viewPager)
    }

    private val progressBar: ProgressBar by lazy {
        findViewById(R.id.progress)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        initData()
    }

    private fun initViews() {
        //페이지 알파값을 통해 화면 전환 시 흐려지는 효과를 부여하는 함수
        viewPager.setPageTransformer { page, position ->
            when {
                position.absoluteValue >= 1F -> {
                    // absoluteValue = 절대값 의미
                    //사용자의 인지 범위 바깥
                    page.alpha = 0F
                }
                position == 0F -> { //화면 중앙
                    page.alpha = 1F
                }
                else -> { //화면 중앙 View와 인접한 View에 대한 처리
                    page.alpha = 1F - 2 * position.absoluteValue
                }
            }
        }
    }

    private fun initData() {
        val remoteConfig = Firebase.remoteConfig
        remoteConfig.setConfigSettingsAsync(
            remoteConfigSettings {
                minimumFetchIntervalInSeconds = 0
                //서버에서 Block 처리하지 않는 이상 앱을 실행시킬때마다 자동 패치
            }
        )
        remoteConfig.fetchAndActivate().addOnCompleteListener {
            //서버와 비동기적이라서 Listener 필요

            progressBar.visibility = View.GONE
            //패치가 완료되면 ProgressBar 삭제

            if(it.isSuccessful) { //서버와의 연결에 성공 시
                val quotes = parseQuotesJson(remoteConfig.getString("quotes"))
                val isNameRevealed = remoteConfig.getBoolean("is_name_revealed")
                displayQuotesPager(quotes, isNameRevealed)
                //파이어베이스 서버의 json 데이터들을 파싱하여 화면에 그 내용이 나타나도록 함
            }
        }
    }

    private fun parseQuotesJson(json: String): List<Quote> {
        val jsonArray = JSONArray(json)
        //json 파싱

        var jsonList = emptyList<JSONObject>()
        //파싱한 데이터를 담을 리스트 생성

        for(index in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(index)
            jsonObject?.let {
                jsonList = jsonList + it
                //기존의 json 파싱 데이터 뒤에 연달아 데이터가 붙음
            }
        }

        return jsonList.map {
            Quote(
                quote = it.getString("quote"), it.getString("name")
            )
        }
    }

    private fun displayQuotesPager(quotes: List<Quote>, isNameRevealed: Boolean) {
        val adapter = QuotesPageAdapter(
            quotes = quotes,
            isNameRevealed = isNameRevealed
            //파이어베이스에서 등록한 데이터 값들을 지정 및 호출
        )

        viewPager.adapter = adapter
        viewPager.setCurrentItem(adapter.itemCount/2, false)
        //첫 페이지에서 왼쪽으로도 순환 가능
    }
}