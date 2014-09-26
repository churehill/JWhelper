/**
 * @author churehill
 * @email churehill@163.com
 * @version 1.0
 * @time 2014-07
 * 
 */
package cn.edu.nwpu.jwhelper;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity {

	private EditText userEditText;
	private EditText pwdEditText;
	private Button loginButton;
	private Button clearButton;
	private CheckBox rememberBox;
	private ProgressDialog proDialog;

	private CookieManager cookieManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.activity_main);
		cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
		CookieHandler.setDefault(cookieManager);
		HttpURLConnection.setFollowRedirects(true);

		userEditText = (EditText) findViewById(R.id.txtUserName);
		pwdEditText = (EditText) findViewById(R.id.txtPassword);
		loginButton = (Button) findViewById(R.id.btnSignIn);
		clearButton = (Button) findViewById(R.id.btnClearInput);
		rememberBox = (CheckBox) findViewById(R.id.chkRememberPassword);

		clearButton.setOnClickListener(clearListener);
		loginButton.setOnClickListener(loginListener);

		cookieManager.getCookieStore().removeAll();

		SharedPreferences share = getSharedPreferences("JWDATA", MODE_PRIVATE);
		String userName = share.getString("username", "");
		String password = share.getString("password", "");
		if (!userName.equals(""))
			userEditText.setText(userName);
		if (!password.equals("")) {
			pwdEditText.setText(password);
			rememberBox.setChecked(true);
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	private OnClickListener loginListener = new OnClickListener() {

		@Override
		public void onClick(View v) {

			if (userEditText.getText().toString().isEmpty()) {
				Toast.makeText(MainActivity.this, "用户名不能为空", Toast.LENGTH_SHORT)
						.show();
				return;
			}
			if (pwdEditText.getText().toString().isEmpty()) {
				Toast.makeText(MainActivity.this, "密码不能为空", Toast.LENGTH_SHORT)
						.show();
				return;
			}

			SharedPreferences share = getSharedPreferences("JWDATA",
					MODE_PRIVATE);
			SharedPreferences.Editor editor = share.edit();
			editor.putString("username", userEditText.getText().toString());
			if (rememberBox.isChecked())
				editor.putString("password", pwdEditText.getText().toString());
			else
				editor.putString("password", "");
			editor.commit();

			Log.d("JWhelper", "start progress");
			proDialog = ProgressDialog.show(MainActivity.this, "正在登陆", "",
					true, false);
			Log.d("JWhelper", "create thread");
			// Thread loginThread = new Thread(new LoginHandler());
			// loginThread.start();

			new LoginTask().execute();

		}
	};

	private OnClickListener clearListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			userEditText.setText("");
			pwdEditText.setText("");
		}
	};

	private String InputStreamtoString(InputStream stream) throws IOException {

		BufferedReader in = new BufferedReader(new InputStreamReader(stream));
		String inputLine;
		StringBuffer buffer = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			buffer.append(inputLine);
		}
		in.close();
		return buffer.toString();
	}

	private class LoginTask extends AsyncTask<Void, String, Boolean> {

		@Override
		protected Boolean doInBackground(Void... params) {
			String userName = userEditText.getText().toString();
			String password = pwdEditText.getText().toString();
			Log.d("JWhelper", "new thread");

			cookieManager.getCookieStore().removeAll();

			try {
				URL url = new URL("http://222.24.192.69/mhLogin.jsp");

				HttpURLConnection conn = (HttpURLConnection) url
						.openConnection();
				// conn.setReadTimeout(5000);
				Log.d("JWhelper", "create connectiond done");

				int status = conn.getResponseCode();
				Log.d("JWhelper", "connect");
				if (status != HttpURLConnection.HTTP_OK)
					throw new Exception("网络有问题");

				String htmlstr = InputStreamtoString(conn.getInputStream());
				Log.d("JWhelper", "URL Content... \n" + htmlstr);

				Pattern pattern = Pattern
						.compile("name=\"lt\"\\s+value=\"([a-z0-9A-Z_-]+)\"");
				Matcher matcher = pattern.matcher(htmlstr);
				if (!matcher.find())
					throw new Exception("网络有问题");

				conn = (HttpURLConnection) new URL(
						"http://cas.nwpu.edu.cn/cas/login").openConnection();
				conn.setRequestMethod("POST");
				conn.setUseCaches(false);
				conn.setDoInput(true);
				conn.setDoOutput(true);

				DataOutputStream wr = new DataOutputStream(
						conn.getOutputStream());
				wr.writeBytes("encodedService=http%253a%252f%252f222.24.192.69%252fmhLogin.jsp");
				wr.writeBytes("&service=http%3A%2F%2F222.24.192.69%2FmhLogin.jsp&serviceName=null&loginErrCnt=0&username=");
				wr.writeBytes(userName + "&password=" + password + "&lt="
						+ matcher.group(1));

				wr.flush();
				wr.close();

				htmlstr = InputStreamtoString(conn.getInputStream());

				Log.d("JWhelper", "URL Content... \n" + htmlstr);
				pattern = Pattern
						.compile("window\\.location\\.href=\"([^\" ]+)\"");
				matcher = pattern.matcher(htmlstr);
				if (!matcher.find())
					throw new Exception("用户名或密码错误");

				conn = (HttpURLConnection) new URL(matcher.group(1))
						.openConnection();
				status = conn.getResponseCode();
				if (status != HttpURLConnection.HTTP_OK)
					throw new Exception("登入教务系统失败");
				publishProgress("登陆成功");
				return true;
			} catch (Exception e) {
				e.printStackTrace();
				Log.d("JWhelper", "mess up");
				publishProgress(e.getLocalizedMessage());

				return false;
				// Message msg = Message.obtain();
				// Bundle bun = new Bundle();
				// bun.putString("info", e.getLocalizedMessage());
				// msg.setData(bun);
			}
		}

		@Override
		protected void onProgressUpdate(String... msgs) {
			Toast.makeText(MainActivity.this, msgs[0], Toast.LENGTH_SHORT)
					.show();
		}

		@Override
		protected void onPostExecute(Boolean result) {
			proDialog.dismiss();
			if (result) {
				// startActivity(new
				// Intent("cn.edu.nwpu.jwhelper.ScoresActivity"));
				startActivity(new Intent(getBaseContext(), ScoresActivity.class));
			}
		}
	}

}
