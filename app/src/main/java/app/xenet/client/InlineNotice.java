package app.xenet.client;
import android.content.Context;import android.graphics.Typeface;import android.graphics.drawable.GradientDrawable;import android.view.*;import android.widget.*;
/** A single dismissible notice in normal layout flow, never an overlay or system Toast. */
public final class InlineNotice extends LinearLayout {
 private final TextView text;private String current="";
 private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
 public InlineNotice(Context context,int ink,int card,int border,Typeface font,boolean gutters){super(context);setOrientation(VERTICAL);setPadding(gutters?dp(20):0,0,gutters?dp(20):0,dp(12));setVisibility(GONE);
  LinearLayout surface=new LinearLayout(context);surface.setGravity(Gravity.CENTER_VERTICAL);surface.setMinimumHeight(dp(64));surface.setPadding(dp(12),dp(8),0,dp(8));GradientDrawable bg=new GradientDrawable();bg.setColor(card);bg.setCornerRadius(dp(12));bg.setStroke(dp(1),border);surface.setBackground(bg);addView(surface,new LayoutParams(-1,-2));
  ImageView icon=new ImageView(context);icon.setImageResource(R.drawable.ic_info);icon.setColorFilter(ink);icon.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);LayoutParams glyph=new LayoutParams(dp(20),dp(20));glyph.rightMargin=dp(10);surface.addView(icon,glyph);
  text=new TextView(context);text.setTypeface(font);text.setTextSize(14);text.setTextColor(ink);text.setIncludeFontPadding(false);text.setLineSpacing(0,1.15f);text.setAccessibilityLiveRegion(ACCESSIBILITY_LIVE_REGION_POLITE);surface.addView(text,new LayoutParams(0,-2,1));
  ImageButton close=new ImageButton(context);close.setImageResource(R.drawable.ic_close);close.setColorFilter(ink);close.setBackgroundResource(android.R.color.transparent);close.setPadding(dp(12),dp(12),dp(12),dp(12));close.setContentDescription("Dismiss message");surface.addView(close,new LayoutParams(dp(44),dp(44)));close.setOnClickListener(v->clear());
 }
 public void showMessage(String message){if(message==null||message.isEmpty()||message.equals(current)&&getVisibility()==VISIBLE)return;current=message;text.setText(message);setVisibility(VISIBLE);}
 public void clear(){current="";text.setText("");setVisibility(GONE);}
}
