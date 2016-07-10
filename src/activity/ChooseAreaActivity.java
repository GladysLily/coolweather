package activity;

import java.util.ArrayList;
import java.util.List;
import java.util.ListResourceBundle;

import util.HttpCallbackListener;
import util.HttpUtil;
import util.Utility;

import com.example.coolweather.R;//ע������android.R

import model.City;
import model.County;
import model.Province;

import db.CoolWeatherDB;
//import android.R;
//import android.R;
import android.app.Activity;
import android.app.DownloadManager.Query;
import android.app.DownloadManager.Request;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.style.SuperscriptSpan;
import android.view.View;
import android.view.Window;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
//import android.view.*;
import android.widget.AdapterView;

public class ChooseAreaActivity extends Activity {
	
	public static final int LEVEL_PROVINCE=0;
	public static final int LEVEL_CITY=1;
	public static final int LEVEL_COUNTY=2;
	
	private ProgressDialog progressDialog;
	private TextView titleText;
	private ListView listView;
	private ArrayAdapter<String> adapter;
	private CoolWeatherDB coolWeatherDB;
	private List<String> datalList=new ArrayList<String>();
	
	//ʡ�б�
	private List<Province> provinceList;
	//���б�
	private List<City> cityList;
	//���б�
	private List<County> countylList;
	//ѡ�е�ʡ��
	private Province selectedProvince;
	//ѡ�еĳ���
	private City selectedCity;
	//ѡ�еļ���
	private int currentLevel;

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.choose_area);
		listView=(ListView)findViewById(R.id.list_view);
		titleText=(TextView)findViewById(R.id.title_text);
		adapter=new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, datalList);
		listView.setAdapter(adapter);
		coolWeatherDB=CoolWeatherDB.getInstance(this);
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View view, int index,
					long arg3) {
				// TODO Auto-generated method stub
					if(currentLevel==LEVEL_PROVINCE){
						selectedProvince=provinceList.get(index);
						queryCities();
					}
					else if (currentLevel==LEVEL_CITY) {
						selectedCity=cityList.get(index);
						queryCounties();
					}
				}	
		});
		queryProvinces();//����ʡ������
	}
	
	//��ѯȫ�����е�ʡ�����ȴ����ݿ��ѯ�����û�в�ѯ����ȥ�������ϲ�ѯ
	private void queryProvinces() {
		provinceList=coolWeatherDB.loadProvinces();
		if(provinceList.size()>0){
			datalList.clear();
			for(Province province:provinceList){
				datalList.add(province.getProvinceName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			//titleText.setText("�й�");
			currentLevel=LEVEL_PROVINCE;
		}
		else{
			queryFromServer(null, "province");
		}
	}
	//��ѯѡ��ʡ�����е��У����ȴ����ݿ��ѯ�����û�в鵽��ȥ�������ϲ�ѯ
	private void queryCities() {
		cityList=coolWeatherDB.loadCities(selectedProvince.getId());
		if(cityList.size()>0){
			datalList.clear();
			for(City city:cityList){
				datalList.add(city.getCityName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText(selectedProvince.getProvinceName());
			currentLevel=LEVEL_CITY;
		}
		else {
			queryFromServer(selectedProvince.getProvinceCode(), "city");
		}
	}
	//��ѯѡ���������е��أ����ȴ����ݿ��ѯ�����û�в�ѯ����ȥ�������ϲ�ѯ
	private void queryCounties() {
		countylList=coolWeatherDB.loadCounties(selectedCity.getId());
		if(countylList.size()>0){
			datalList.clear();
			for(County county:countylList){
				datalList.add(county.getCountyName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText(selectedCity.getCityName());
			currentLevel=LEVEL_COUNTY;
		}
		else {
			queryFromServer(selectedCity.getCityCode(), "county");
		}
	}
	//���ݴ���Ĵ��ź����ʹӷ������ϲ�ѯʡ��������
	private void queryFromServer(final String code,final String type) {
		String address;
		if(!TextUtils.isEmpty(code)){
			address="http:www.weather.com.cn/data/list3/city"+code+".xml";
		}
		else {
			address="http:www.weather.com.cn/data/list3/city.xml";
		}
		showProgressDialog();
		HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
		
			@Override
			public void onFinish(String response) {
				// TODO Auto-generated method stub
				boolean result=false;
				if("province".equals(type)){
					result=Utility.handleProvinceResponse(coolWeatherDB, response);
				}
				else if ("city".equals(type)) {
					result=Utility.handleCitiesResponse(coolWeatherDB, response, selectedProvince.getId());
				}
				else if ("county".equals(type)) {
					result=Utility.handleCountiesResponse(coolWeatherDB, response, selectedCity.getId());
				}
				if(result){
					//ͨ��runOnUiThread()�����ص����̴߳����߼�
					runOnUiThread(new Runnable() {
						
						public void run() {
							closeProgressDialog();
							if("province".equals(type)){
								queryProvinces();
							}
							else if ("city".equals(type)) {
								queryCities();
							}
							else if ("county".equals(type)) {
								queryCounties();
							}
						}
					});
				}
			}
			
			@Override
			public void onError(Exception e) {
				// TODO Auto-generated method stub
				//ͨ��runOnUiThread()�����ص����̴߳����߼�
				runOnUiThread(new Runnable() {
					public void run() {
						closeProgressDialog();
						Toast.makeText(ChooseAreaActivity.this, "����ʧ��", Toast.LENGTH_SHORT).show();
					}
				});
			}
		});
		
	}
	
	//��ʾ���ȶԻ���
	private void showProgressDialog() {
		if(progressDialog==null){
			progressDialog=new ProgressDialog(this);
			progressDialog.setMessage("���ڼ���...");
			progressDialog.setCanceledOnTouchOutside(false);
		}
		progressDialog.show();
	}
	
	//�رս��ȶԻ���
	private void closeProgressDialog() {
		if(progressDialog!=null){
			progressDialog.dismiss();
		}
	}
	//����Back���������ݵ�ǰ�ļ������жϣ���ʱӦ�÷������б�ʡ�б�����ֱ���˳�
	@Override
	public void onBackPressed(){
		if(currentLevel==LEVEL_COUNTY){
			queryCities();
		}
		else if (currentLevel==LEVEL_CITY) {
			queryProvinces();
		}
		else {
			finish();
		}
	}
}
