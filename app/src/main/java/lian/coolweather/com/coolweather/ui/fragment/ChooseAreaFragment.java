package lian.coolweather.com.coolweather.ui.fragment;


import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import lian.coolweather.com.coolweather.R;
import lian.coolweather.com.coolweather.db.City;
import lian.coolweather.com.coolweather.db.County;
import lian.coolweather.com.coolweather.db.Province;
import lian.coolweather.com.coolweather.util.HttpUtil;
import lian.coolweather.com.coolweather.util.Utility;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * @description 选择省市县
 * @author lian
 * @time 2019/5/25 16:11
 */
public class ChooseAreaFragment extends Fragment {

    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY = 2;
    private ProgressDialog progressDialog;
    private ArrayAdapter<String> adapter;
    private List<String> dataList = new ArrayList<>();
    /**
     * 省列表
     */
    private List<Province> provinceList;
    /**
     * 市列表
     */
    private List<City> citylist;
    /**
     * 县列表
     */
    private List<County> countylist;
    /**
     * 选中的省份
     */
    private Province selectedProvince;
    /**
     * 选中的城市
     */
    private City selectedCity;
    /**
     * 当前选中的级别
     */
    private int currentLevel;

    private Unbinder unbinder;
    @BindView(R.id.tvTitle)
    TextView mTvTitle;
    @BindView(R.id.btnBack)
    Button mBtnBack;
    @BindView(R.id.list_view)
    ListView mListView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_area, container, false);
        unbinder = ButterKnife.bind(this, view);

        adapter = new ArrayAdapter<>(getContext(),android.R.layout.simple_list_item_1,dataList);
        mListView.setAdapter(adapter);
        EventBus.getDefault().register(this);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mListView.setOnItemClickListener((parent, view, position, id) -> {
            if (currentLevel == LEVEL_PROVINCE){
                selectedProvince = provinceList.get(position);
                queryCities();
            }else if (currentLevel == LEVEL_CITY){
                selectedCity = citylist.get(position);
                queryCounties();
            }
        });
        mBtnBack.setOnClickListener(v -> {
            if (currentLevel == LEVEL_COUNTY){
                queryCities();
            }else if (currentLevel == LEVEL_CITY){
                queryProvinces();
            }
        });
        queryProvinces();
    }

    /**
     * @description 查询省
     * @author lian
     * @time 2019/5/25 15:44
     */
    private void queryProvinces(){
        mTvTitle.setText("中国");
        mBtnBack.setVisibility(View.GONE);
        provinceList = DataSupport.findAll(Province.class);
        if (provinceList.size() >0){
            dataList.clear();
            for (Province province: provinceList){
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            mListView.setSelection(0);
            currentLevel = LEVEL_PROVINCE;
        }else {
            String address = "http://guolin.tech/api/china";
            queryFromServer(address,"province");
        }
    }

    /**
     * @description 查询市
     * @author lian
     * @time 2019/5/25 15:44
     */
    private void queryCities(){
        mTvTitle.setText(selectedProvince.getProvinceName());
        mBtnBack.setVisibility(View.VISIBLE);
        citylist = DataSupport.where("provinceid = ?",String.valueOf(selectedProvince
        .getId())).find(City.class);
        if (citylist.size() >0){
            dataList.clear();
            for (City city: citylist){
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            mListView.setSelection(0);
            currentLevel = LEVEL_CITY;
        }else {
            int provinceCode = selectedProvince.getProvinceCode();
            String address = "http://guolin.tech/api/china"+"/"+provinceCode;
            Log.e("ttt",address);
            queryFromServer(address,"city");
        }
    }

    /**
     * @description 查询县
     * @author lian
     * @time 2019/5/25 15:44
     */
    private void queryCounties(){
        mTvTitle.setText(selectedCity.getCityName());
        mBtnBack.setVisibility(View.VISIBLE);
        countylist = DataSupport.where("cityid = ?",String.valueOf(selectedCity
                .getId())).find(County.class);
        if (countylist.size() >0){
            dataList.clear();
            for (County county: countylist){
                dataList.add(county.getCountryName());
            }
            adapter.notifyDataSetChanged();
            mListView.setSelection(0);
            currentLevel = LEVEL_COUNTY;
        }else {
            int provinceCode = selectedProvince.getProvinceCode();
            int cityCode = selectedCity.getCityCode();
            String address = "http://guolin.tech/api/china"+provinceCode +
                    "/" + cityCode;
            queryFromServer(address,"city");
        }
    }

    /**
     * @description 从服务器上查询省市县数据
     * @author lian
     * @time 2019/5/25 15:56
     */
    private void queryFromServer(String address, final String type){
        showProgressDialog();
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                getActivity().runOnUiThread(() -> {
                    closeProgressDialog();
                    Toast.makeText(getContext(),"加载失败",Toast.LENGTH_SHORT)
                            .show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText = response.body().string();
                boolean result = false;
                if ("province".equals(type)){
                    result = Utility.handleProvinceResponse(responseText);
                }else if ("city".equals(type)){
                    result = Utility.handleCityResponse(responseText,selectedProvince.getId());
                }else if ("county".equals(type)){
                    result = Utility.handleCountryResponse(responseText,selectedCity.getId());
                }
                if (result){
                    getActivity().runOnUiThread(() -> {
                        closeProgressDialog();
                        if ("province".equals(type)){
                            queryProvinces();
                        }else if ("city".equals(type)){
                            queryCities();
                        }else if ("county".equals(type)){
                            queryCounties();
                        }
                    });
                }
            }
        });
    }
    
    /**
     * 显示进度对话框
     */
    private void showProgressDialog(){
        if (progressDialog == null){
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
    }

    /**
     * 关闭对话框
     */
    private void closeProgressDialog(){
        if (progressDialog != null){
            progressDialog.dismiss();
        }
    }


    /**
     * @description 解绑
     * @author lian
     * @time 2019/5/25 15:31
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
        EventBus.getDefault().unregister(this);
    }
}
