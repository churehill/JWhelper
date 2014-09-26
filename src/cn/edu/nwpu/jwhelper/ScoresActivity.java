/**
 * @author churehill
 * @email churehill@163.com
 * @version 1.0
 * @time 2014-07
 * 
 */
package cn.edu.nwpu.jwhelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class ScoresActivity extends ListActivity {

	private ProgressDialog proDialog;
	private ArrayAdapter<String> adapter;
	private ArrayList<String> scoresArray;

	// public static TabActivity context;
	// private String[] scoresStrings = {};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// context = this;

		setContentView(R.layout.activity_scores);

		scoresArray = new ArrayList<String>();
		adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, scoresArray);
		setListAdapter(adapter);
		proDialog = ProgressDialog.show(ScoresActivity.this, "正在获取成绩数据", "",
				true, false);

		// SharedPreferences share = getSharedPreferences("SCOREDATA",
		// MODE_PRIVATE);

		new GetDataTask().execute();

	}

	@Override
	public void onListItemClick(ListView parent, View v, int position, long id) {

		// super.onListItemClick(parent, v, position, id);

		SharedPreferences share = getSharedPreferences("SCOREDATA",
				MODE_PRIVATE);
		String str = share.getString("" + position, "");
		String[] items = str.split("\n");
		items[0] = "课程号:" + items[0].trim();
		items[1] = "课序号:" + items[1];
		items[2] = "课程名:" + items[2];
		items[3] = "英文名:" + items[3];
		items[4] = "学分:" + items[4];
		items[5] = "属性:" + items[5];
		items[6] = "成绩:" + items[6];

		Toast.makeText(this, TextUtils.join("\n", items), Toast.LENGTH_LONG)
				.show();

		// new AlertDialog.Builder(getParent())
		// .setTitle("成绩详情:")
		// .setItems(items, null)
		// .setPositiveButton("确认",null)
		// .create().show();
	}

	public void onRefreshClick(View view) {

		proDialog = ProgressDialog.show(ScoresActivity.this, "正在获取成绩数据", "",
				true, false);
		new GetDataTask().execute();
	}

	private String addScore(String str) {

		Log.d("SCORE", "begin to replace");
		// str.replaceAll("<.*?>", "");
		// str = str.replaceAll("<.*align.*?>", "");
		// str = str.replaceAll("</P>", "");
		// Pattern pattern = Pattern.compile("<td.*?>|<p.*?>");
		// Matcher matcher = pattern.matcher(str);
		// matcher.replaceAll("");
		str = str.replace("<td align=\"center\">", "");
		str = str.replace("<p align=\"center\">", "");
		str = str.replace("<td>", "");
		Log.d("SCORE", str);

		str = str.replace("</P>", "");
		str = str.replace("&nbsp;", "");
		Log.d("SCORE", str);

		String[] data = str.split("\\s*</td>\\s*");
		Log.d("SCORE", "finish to replace");
		String itemstr = "";
		String line = data[2];
		itemstr += line + "\n";
		Log.d("SCORE", itemstr);
		/*
		 * if(line.length() > 6) itemstr += line.substring(0,6)+".."; else
		 * if(line.length() < 6) { itemstr += line; for(int
		 * i=0;i<6-line.length();i++) itemstr += "  "; }
		 */
		line = data[4];
		itemstr += " 学分: " + line;
		for (int i = 0; i < 3 - line.length(); i++)
			itemstr += " ";
		Log.d("SCORE", itemstr);
		line = data[6];
		itemstr += "      分数: " + line.replace("&nbsp;", "");
		Log.d("SCORE", itemstr);

		scoresArray.add(itemstr);

		return TextUtils.join("\n", data);

	}

	private String InputStreamtoString(InputStream stream) throws IOException {

		// InputStreamReader isr = new InputStreamReader(stream);
		// String str = "";
		// char[] inputBuffer = new char[2000];
		// int charRead;
		// while((charRead = isr.read(inputBuffer)) > 0) {
		// String readString = String.copyValueOf(inputBuffer, 0, charRead);
		// str += readString;
		// inputBuffer = new char[2000];
		// //Log.d("SCORE",readString);
		// }
		// stream.close();
		//
		// return str;

		BufferedReader in = new BufferedReader(new InputStreamReader(stream,
				"GBK"));
		String inputLine;
		StringBuffer buffer = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			buffer.append(inputLine);
		}
		Log.d("SCORE", "inclose");
		in.close();
		return buffer.toString();// new
									// String(buffer.toString().getBytes("GBK"),"UTF-8");
	}

	private class GetDataTask extends AsyncTask<Void, String, Boolean> {
		@Override
		protected Boolean doInBackground(Void... params) {

			try {
				URL url = new URL(
						"http://222.24.192.69/gradeLnAllAction.do?type=ln&oper=fainfo&fajhh=4308");

				HttpURLConnection conn = (HttpURLConnection) url
						.openConnection();
				conn.setReadTimeout(50000);

				int status = conn.getResponseCode();
				if (status != HttpURLConnection.HTTP_OK)
					throw new Exception("网络有问题");

				Log.d("SCORE", conn.getContentLength() + "");

				String htmlstr = InputStreamtoString(conn.getInputStream());
				Log.d("SCORE", htmlstr);

				// String regex =
				// "<td align=\"center\">\\s*(\\d+)\\s*</td>\\s*";
				// regex += "<td align=\"center\">\\s*(\\d+)\\s*</td>\\s*";
				// regex +=
				// "<td align=\"center\">\\s*([- a-zA-Z0-9_u4E00-u9FA5uFE30-uFFA0]+)\\s*</td>\\s*";
				// regex += "<td align=\"center\">(.*?)</td>\\s*";
				// regex += "<td align=\"center\">\\s*(\\d+)\\s*</td>\\s*";

				String regex = "<tr class=\"odd\".*?>([\\s\\S]*?)</tr>";
				Pattern pattern = Pattern.compile(regex);
				Matcher matcher = pattern.matcher(htmlstr);

				Log.d("SCORE", "get preference");
				SharedPreferences share = getSharedPreferences("SCOREDATA",
						MODE_PRIVATE);
				SharedPreferences.Editor editor = share.edit();
				editor.clear();

				scoresArray.clear();
				int i = 0;
				while (matcher.find()) {
					// Log.d("SCORE","group"+i+ matcher.group(1));

					// addScore(matcher.group(1));
					editor.putString("" + i, addScore(matcher.group(1)));
					i++;
				}
				editor.commit();
				// scoresArray.toArray(scoresStrings);

				return true;
			} catch (Exception e) {
				publishProgress(e.getLocalizedMessage());
				return false;
			}
		}

		@Override
		protected void onProgressUpdate(String... msgs) {
			Toast.makeText(ScoresActivity.this, "网络错误" + msgs[0],
					Toast.LENGTH_SHORT).show();
		}

		@Override
		protected void onPostExecute(Boolean result) {
			proDialog.dismiss();
			adapter.notifyDataSetChanged();
		}
	}
}
