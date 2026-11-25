package com.greenmate.smartfarming;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    private final int GREEN = Color.rgb(23,75,54);
    private final int PALE = Color.rgb(235,245,235);
    private LinearLayout root, content, nav;
    private String currentPage = "Home";
    private final ArrayList<JSONObject> reminders = new ArrayList<>();
    private final String PREFS = "greenmate_data";
    private String pickedDate = "", pickedTime = "08:00";
    private EditText titleInput, notesInput, locationInput;
    private Spinner typeSpinner;
    private int editingIndex = -1;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(GREEN);
        getWindow().setNavigationBarColor(GREEN);
        loadReminders();
        buildShell();
        showHome();
        askNotificationPermission();
    }

    private void buildShell() {
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Color.rgb(247,249,245));
        setContentView(root);
        TextView brand = new TextView(this); brand.setText("🌿  GreenMate"); brand.setTextColor(Color.WHITE);
        brand.setTextSize(23); brand.setTypeface(null, Typeface.BOLD); brand.setPadding(20,18,20,18); brand.setBackgroundColor(GREEN);
        root.addView(brand, new LinearLayout.LayoutParams(-1,-2));
        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(18,18,18,18);
        scroll.addView(content); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        nav = new LinearLayout(this); nav.setBackgroundColor(Color.WHITE); nav.setElevation(8); nav.setGravity(Gravity.CENTER);
        String[] pages={"Home","Weather","Crops","Reminders"};
        for(String p:pages){ Button b=navButton(p); nav.addView(b,new LinearLayout.LayoutParams(0,58,1)); }
        root.addView(nav,new LinearLayout.LayoutParams(-1,-2));
    }
    private Button navButton(String name){
        Button b=new Button(this); b.setText(name); b.setTextSize(11); b.setAllCaps(false); b.setTextColor(GREEN);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));
        b.setOnClickListener(v->{currentPage=name;renderPage();}); return b;
    }
    private void renderPage(){content.removeAllViews();if(currentPage.equals("Home"))showHome();else if(currentPage.equals("Weather"))showWeather();else if(currentPage.equals("Crops"))showCrops();else showReminders();}
    private TextView text(String s,int size,boolean bold,int color){
        TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(null,Typeface.BOLD);t.setPadding(0,5,0,8);return t;
    }
    private void heading(String s){content.addView(text(s,22,true,GREEN));}
    private void paragraph(String s){content.addView(text(s,14,false,Color.DKGRAY));}
    private void gap(){View v=new View(this);content.addView(v,new LinearLayout.LayoutParams(1,10));}
    private Button button(String s,Runnable action){
        Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(Color.WHITE);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(GREEN));b.setOnClickListener(v->action.run());return b;
    }
    private EditText edit(String hint){
        EditText e=new EditText(this);e.setSingleLine(true);e.setHint(hint);e.setTextSize(15);
        e.setPadding(12,8,12,8);e.setBackgroundTintList(android.content.res.ColorStateList.valueOf(GREEN));return e;
    }
    private void showHome(){
        heading("Good day, farmer 👩‍🌾");paragraph("Your farm, organized. Weather, crop stages and farming reminders in one simple app.");gap();
        card("☀  WEATHER INFORMATION","Check local conditions and a 5-day forecast.",()->{currentPage="Weather";renderPage();});
        card("🌾  CROP CALENDAR","Explore growing stages for common crops.",()->{currentPage="Crops";renderPage();});
        card("🔔  FARMING REMINDERS","Create and manage important farm tasks.",()->{currentPage="Reminders";renderPage();});
        gap();heading("Quick overview");paragraph("Saved reminders: "+reminders.size());
        paragraph("Crop guides available: 5");
        paragraph("Tip: Use local agricultural advice to confirm crop timing and practices.");
    }
    private void card(String title,String desc,Runnable action){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(16,12,16,12);
        box.setBackgroundColor(Color.WHITE);box.setElevation(2);
        box.addView(text(title,16,true,GREEN));box.addView(text(desc,13,false,Color.DKGRAY));
        box.addView(button("Open",action));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=12;content.addView(box,lp);
    }
    private void showWeather(){
        heading("Local weather");paragraph("Enter a city, town or village to view current weather and a 5-day forecast.");
        locationInput=edit("Enter location (e.g. Chennai)");locationInput.setText("Chennai");content.addView(locationInput);
        gap();content.addView(button("Get weather",()->fetchWeather(locationInput.getText().toString().trim())));
        gap();
    }
    private void fetchWeather(String place){
        if(place.isEmpty()){toast("Enter a location first");return;}
        paragraph("Loading weather for "+place+"…");
        new Thread(()->{
            try{
                String q=URLEncoder.encode(place,"UTF-8");
                JSONObject geo=getJson("https://geocoding-api.open-meteo.com/v1/search?name="+q+"&count=1&language=en&format=json");
                JSONArray results=geo.optJSONArray("results");if(results==null||results.length()==0)throw new Exception("Location not found. Check the spelling.");
                JSONObject loc=results.getJSONObject(0);
                String api="https://api.open-meteo.com/v1/forecast?latitude="+loc.getDouble("latitude")+"&longitude="+loc.getDouble("longitude")+
                    "&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max,weather_code&timezone=auto&forecast_days=5";
                JSONObject data=getJson(api),cur=data.getJSONObject("current"),daily=data.getJSONObject("daily");
                String label=loc.optString("name")+", "+loc.optString("country");
                String result="📍 "+label+"\n\n"+weatherIcon(cur.optInt("weather_code"))+"  "+Math.round(cur.optDouble("temperature_2m"))+"°C — "+weatherName(cur.optInt("weather_code"))+
                    "\nFeels like: "+Math.round(cur.optDouble("apparent_temperature"))+"°C"+
                    "\nHumidity: "+cur.optInt("relative_humidity_2m")+"%"+
                    "\nWind: "+Math.round(cur.optDouble("wind_speed_10m"))+" km/h"+
                    "\n\n5-DAY FORECAST";
                JSONArray dates=daily.getJSONArray("time"),max=daily.getJSONArray("temperature_2m_max"),min=daily.getJSONArray("temperature_2m_min"),codes=daily.getJSONArray("weather_code"),rain=daily.getJSONArray("precipitation_probability_max");
                for(int i=0;i<dates.length();i++)result+="\n"+dates.getString(i)+"  "+weatherIcon(codes.getInt(i))+"  "+Math.round(max.getDouble(i))+"° / "+Math.round(min.getDouble(i))+"°  Rain: "+rain.optInt(i)+"%";
                final String output=result;runOnUiThread(()->{content.addView(text(output,15,false,Color.DKGRAY));});
            }catch(Exception ex){runOnUiThread(()->toast(ex.getMessage()==null?"Weather service unavailable":ex.getMessage()));}
        }).start();
    }
    private JSONObject getJson(String address)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(address).openConnection();c.setConnectTimeout(10000);c.setReadTimeout(10000);c.setRequestMethod("GET");
        try(java.io.InputStream in=c.getInputStream();java.util.Scanner sc=new java.util.Scanner(in).useDelimiter("\\A")){return new JSONObject(sc.hasNext()?sc.next():"{}");}
        finally{c.disconnect();}
    }
    private String weatherName(int c){switch(c){case 0:return "Clear sky";case 1:case 2:return "Partly cloudy";case 3:return "Overcast";case 45:case 48:return "Fog";case 51:case 53:case 55:return "Drizzle";case 61:case 63:case 65:return "Rain";case 80:case 81:case 82:return "Rain showers";case 95:return "Thunderstorm";default:return "Variable conditions";}}
    private String weatherIcon(int c){if(c==0)return "☀";if(c==1||c==2)return "🌤";if(c==3)return "☁";if(c>=51&&c<=82)return "🌧";if(c==95)return "⛈";return "🌫";}
    private void showCrops(){
        heading("Crop calendar");paragraph("Select a crop to view its general growing stages. These are guides, not location-specific prescriptions.");
        String[] names={"Rice","Maize","Wheat","Cotton","Tomato"};
        for(String name:names)content.addView(button(name,()->showCropDetail(name)));
        gap();showCropDetail("Rice");
    }
    private void showCropDetail(String crop){
        String[][] stages;
        String duration;
        switch(crop){
            case "Maize":duration="Approx. 80–120 days (varies by variety)";stages=new String[][]{{"Before planting","Prepare the field and check suitable planting conditions."},{"Sowing","Sow quality seeds at recommended spacing."},{"Seedling stage","Check germination and establishment."},{"Vegetative growth","Monitor weeds, nutrients and moisture."},{"Flowering and grain formation","Observe tasseling, silking and grain development."},{"Maturity","Check maturity and plan harvest and storage."}};break;
            case "Wheat":duration="Approx. 110–150 days (varies by variety)";stages=new String[][]{{"Field preparation","Prepare field and plan sowing for the local season."},{"Sowing","Use suitable seed and local sowing practices."},{"Crown root development","Observe establishment and moisture."},{"Tillering and stem growth","Monitor weeds and nutrient needs."},{"Flowering and grain filling","Observe development and water needs."},{"Maturity and harvest","Check grain maturity and plan harvest."}};break;
            case "Cotton":duration="Approx. 150–180+ days (varies by variety)";stages=new String[][]{{"Field preparation","Prepare field and review local sowing advice."},{"Sowing and establishment","Sow suitable seed and monitor germination."},{"Vegetative growth","Observe growth and manage weeds as advised."},{"Flowering and boll formation","Monitor flowering, boll development and crop health."},{"Boll opening","Observe boll opening and plan picking."},{"Harvest and post-harvest","Pick mature kapas and arrange safe storage."}};break;
            case "Tomato":duration="Approx. 90–140 days (varies by variety)";stages=new String[][]{{"Nursery preparation","Prepare a suitable nursery and healthy seedlings."},{"Transplanting","Transplant seedlings at the locally recommended stage."},{"Vegetative growth","Support plants and monitor moisture and weeds."},{"Flowering","Observe flowering and maintain suitable crop care."},{"Fruit development","Monitor fruit growth and inspect plants."},{"Harvesting","Harvest fruits at the desired maturity stage."}};break;
            default:duration="Approx. 110–150 days (varies by variety)";stages=new String[][]{{"Before planting","Prepare and level the field according to local practice."},{"Planting","Sow seeds or transplant healthy seedlings at the locally recommended time."},{"Early growth","Observe seedling establishment and plan water management."},{"Vegetative growth","Monitor weeds, crop growth and nutrient needs."},{"Reproductive stage","Observe flowering and grain development; monitor water needs."},{"Maturity","Check grain maturity and plan harvesting and post-harvest handling."}};
        }
        content.addView(text("🌱  "+crop+" — "+duration,17,true,GREEN));
        for(int i=0;i<stages.length;i++){content.addView(text("STAGE "+(i+1)+"  •  "+stages[i][0],14,true,GREEN));content.addView(text(stages[i][1],13,false,Color.DKGRAY));}
    }
    private void showReminders(){
        heading("Farming reminders");paragraph("Add a task and choose when you want to be reminded.");gap();
        titleInput=edit("Task title (e.g. Check irrigation)");content.addView(titleInput);
        typeSpinner=new Spinner(this);String[] types={"Irrigation","Fertilizer application","Field inspection","Harvest preparation","Other"};
        typeSpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,types));content.addView(typeSpinner);
        gap();if(pickedDate.isEmpty())pickedDate=new SimpleDateFormat("yyyy-MM-dd",Locale.getDefault()).format(new Date());
        content.addView(button("Choose date: "+pickedDate,()->{
            Calendar c=Calendar.getInstance();new DatePickerDialog(this,(v,y,m,d)->{pickedDate=String.format(Locale.US,"%04d-%02d-%02d",y,m+1,d);showReminders();},c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH)).show();
        }));
        content.addView(button("Choose time: "+pickedTime,()->{
            Calendar c=Calendar.getInstance();new TimePickerDialog(this,(v,h,m)->{pickedTime=String.format(Locale.US,"%02d:%02d",h,m);showReminders();},c.get(Calendar.HOUR_OF_DAY),c.get(Calendar.MINUTE),false).show();
        }));
        notesInput=edit("Optional notes");content.addView(notesInput);gap();
        content.addView(button(editingIndex<0?"Save reminder":"Update reminder",()->saveReminder()));
        if(editingIndex>=0)content.addView(button("Cancel edit",()->{editingIndex=-1;showReminders();}));
        gap();heading("Your saved tasks");
        if(reminders.isEmpty())paragraph("No reminders yet. Add your first farming task above.");
        for(int i=0;i<reminders.size();i++){
            final int index=i;JSONObject r=reminders.get(i);
            paragraph("• "+r.optString("title")+"\n"+r.optString("type")+"  |  "+r.optString("date")+" at "+r.optString("time")+
                    (r.optString("notes").isEmpty()?"":"\n"+r.optString("notes")));
            LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);
            Button edit=button("Edit",()->{editingIndex=index;pickedDate=r.optString("date");pickedTime=r.optString("time");showReminders();titleInput.setText(r.optString("title"));notesInput.setText(r.optString("notes"));typeSpinner.setSelection(Math.max(0,typesIndex(r.optString("type"))));});
            Button del=button("Delete",()->{cancelAlarm(r);reminders.remove(index);persist();showReminders();});
            actions.addView(edit,new LinearLayout.LayoutParams(0,-2,1));actions.addView(del,new LinearLayout.LayoutParams(0,-2,1));content.addView(actions);
        }
    }
    private int typesIndex(String type){String[] a={"Irrigation","Fertilizer application","Field inspection","Harvest preparation","Other"};for(int i=0;i<a.length;i++)if(a[i].equals(type))return i;return 0;}
    private void saveReminder(){
        String title=titleInput.getText().toString().trim();if(title.isEmpty()){toast("Please enter a task title");return;}
        try{
            JSONObject r=new JSONObject();r.put("id",editingIndex>=0?reminders.get(editingIndex).optInt("id"):(int)(System.currentTimeMillis()&0x7fffffff));
            r.put("title",title);r.put("type",typeSpinner.getSelectedItem().toString());r.put("date",pickedDate);r.put("time",pickedTime);r.put("notes",notesInput.getText().toString().trim());
            if(editingIndex>=0){cancelAlarm(reminders.get(editingIndex));reminders.set(editingIndex,r);editingIndex=-1;}else reminders.add(r);
            scheduleAlarm(r);persist();toast("Reminder saved");showReminders();
        }catch(Exception e){toast("Could not save reminder");}
    }
    private void scheduleAlarm(JSONObject r){
        try{
            String[] d=r.getString("date").split("-"),t=r.getString("time").split(":");
            Calendar c=Calendar.getInstance();c.set(Integer.parseInt(d[0]),Integer.parseInt(d[1])-1,Integer.parseInt(d[2]),Integer.parseInt(t[0]),Integer.parseInt(t[1]),0);
            if(c.getTimeInMillis()<System.currentTimeMillis()){toast("Reminder date/time is in the past; it was saved but no alert was scheduled.");return;}
            Intent i=new Intent(this,ReminderReceiver.class);i.putExtra("title",r.getString("title"));i.putExtra("type",r.getString("type"));
            PendingIntent pi=PendingIntent.getBroadcast(this,r.getInt("id"),i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE);if(am!=null)am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,c.getTimeInMillis(),pi);
        }catch(Exception ignored){}
    }
    private void cancelAlarm(JSONObject r){
        try{Intent i=new Intent(this,ReminderReceiver.class);PendingIntent pi=PendingIntent.getBroadcast(this,r.getInt("id"),i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);((AlarmManager)getSystemService(ALARM_SERVICE)).cancel(pi);}catch(Exception ignored){}
    }
    private void loadReminders(){try{String s=getSharedPreferences(PREFS,MODE_PRIVATE).getString("reminders","[]");JSONArray a=new JSONArray(s);for(int i=0;i<a.length();i++)reminders.add(a.getJSONObject(i));}catch(Exception ignored){}}
    private void persist(){JSONArray a=new JSONArray();for(JSONObject r:reminders)a.put(r);getSharedPreferences(PREFS,MODE_PRIVATE).edit().putString("reminders",a.toString()).apply();}
    private void askNotificationPermission(){if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},44);}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
}
