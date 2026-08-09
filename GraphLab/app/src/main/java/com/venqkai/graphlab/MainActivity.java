package com.venqkai.graphlab;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class MainActivity extends Activity implements FunctionRowView.Listener {
    private static final int BG=Color.rgb(11,13,18), PANEL=Color.rgb(18,22,31), PANEL2=Color.rgb(24,29,40);
    private static final int TEXT=Color.rgb(239,242,248), MUTED=Color.rgb(153,163,184), BORDER=Color.rgb(45,53,70);
    private static final int ACCENT=Color.rgb(124,157,255), DANGER=Color.rgb(255,112,99);

    private final List<FunctionRowView> rows=new ArrayList<>();
    private GraphView graph;
    private LinearLayout rowBox;
    private EditText minX,maxX;
    private TextView status;
    private SharedPreferences prefs;
    private int nextColor;

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        Window w=getWindow(); w.setStatusBarColor(BG); w.setNavigationBarColor(BG);
        prefs=getSharedPreferences("graphlab",MODE_PRIVATE);
        setContentView(ui()); load(); plot(false);
    }
    @Override protected void onPause(){ super.onPause(); save(); }

    private View ui(){
        LinearLayout root=column(); root.setBackgroundColor(BG); root.setPadding(dp(16),dp(10),dp(16),dp(14));
        LinearLayout head=row(); head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(text("GraphLab",25,TEXT,true),new LinearLayout.LayoutParams(0,-2,1));
        TextView ver=text("1.0.1",11,ACCENT,true); ver.setPadding(dp(10),dp(5),dp(10),dp(5)); ver.setBackground(round(Color.rgb(27,35,55),20,Color.rgb(59,76,116),1)); head.addView(ver); root.addView(head);
        TextView sub=text("Funções personalizáveis e inspeção de coordenadas.",13,MUTED,false); sub.setPadding(dp(2),dp(3),0,dp(12)); root.addView(sub);

        graph=new GraphView(this); graph.setBackground(round(Color.rgb(14,17,24),18,BORDER,1));
        LinearLayout.LayoutParams gp=new LinearLayout.LayoutParams(-1,dp(355)); gp.bottomMargin=dp(7); root.addView(graph,gp);
        TextView hint=text("Toque: x/y • arraste: mover • pinça: zoom • 2 toques: reenquadrar",11,MUTED,false); hint.setGravity(Gravity.CENTER); hint.setPadding(0,0,0,dp(10)); root.addView(hint);

        ScrollView scroll=new ScrollView(this); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout body=column(); body.setPadding(0,dp(2),0,dp(20)); scroll.addView(body,new ScrollView.LayoutParams(-1,-2));
        body.addView(section("FUNÇÕES")); rowBox=column(); body.addView(rowBox);
        Button add=secondary("+ Adicionar função"); add.setOnClickListener(v->addRow("","",nextColor++,true)); LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,dp(44)); ap.bottomMargin=dp(18); body.addView(add,ap);

        body.addView(section("INTERVALO DO EIXO X")); LinearLayout range=row(); minX=number(); maxX=number();
        LinearLayout.LayoutParams a=new LinearLayout.LayoutParams(0,dp(58),1); a.rightMargin=dp(6); LinearLayout.LayoutParams c=new LinearLayout.LayoutParams(0,dp(58),1); c.leftMargin=dp(6);
        range.addView(numberBox("Mínimo",minX),a); range.addView(numberBox("Máximo",maxX),c); body.addView(range);
        TextView syntax=text("Aceita x, pi, e, tau, + − × ÷, ^, %, (), !, sin, cos, tan, sqrt, cbrt, abs, ln, log, exp, min, max, pow e root. Trigonometria usa radianos.",12,MUTED,false); syntax.setPadding(dp(2),dp(10),0,dp(14)); body.addView(syntax);

        LinearLayout actions=row(); Button go=primary("Plotar"); go.setOnClickListener(v->plot(true)); Button reset=secondary("Reenquadrar"); reset.setOnClickListener(v->graph.resetViewport());
        LinearLayout.LayoutParams p1=new LinearLayout.LayoutParams(0,dp(50),1.35f); p1.rightMargin=dp(6); LinearLayout.LayoutParams p2=new LinearLayout.LayoutParams(0,dp(50),1); p2.leftMargin=dp(6); actions.addView(go,p1); actions.addView(reset,p2); body.addView(actions);
        status=text("Pronto.",12,MUTED,false); status.setGravity(Gravity.CENTER); status.setPadding(0,dp(12),0,0); body.addView(status);
        return root;
    }

    private void addRow(String name,String formula,int color,boolean visible){
        FunctionRowView r=new FunctionRowView(this,name,formula,color,visible,this); rows.add(r); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.bottomMargin=dp(8); rowBox.addView(r,p); nextColor=Math.max(nextColor,color+1);
    }
    @Override public void onChanged(){ plot(false); }
    @Override public void onDelete(FunctionRowView r){ if(rows.size()==1){r.clearFields(); setStatus("Adicione uma função para plotar.",MUTED);return;} rows.remove(r); rowBox.removeView(r); plot(false); }
    @Override public void onColorRequested(FunctionRowView r){ colorDialog(r); }

    private void colorDialog(FunctionRowView target){
        GridLayout g=new GridLayout(this); g.setColumnCount(4); g.setPadding(dp(14),dp(8),dp(14),dp(8)); List<Button> buttons=new ArrayList<>();
        for(int i=0;i<GraphView.paletteSize();i++){ Button b=new Button(this); b.setTag(i); b.setMinWidth(0); b.setMinHeight(0); int stroke=i==target.getColorIndex()?Color.WHITE:Color.argb(50,255,255,255); b.setBackground(round(GraphView.colorFor(i),100,stroke,i==target.getColorIndex()?2:1)); GridLayout.LayoutParams p=new GridLayout.LayoutParams(); p.width=dp(50);p.height=dp(50);p.setMargins(dp(6),dp(6),dp(6),dp(6));g.addView(b,p);buttons.add(b); }
        AlertDialog d=new AlertDialog.Builder(this).setTitle("Cor da função").setView(g).setNegativeButton("Cancelar",null).create();
        for(Button b:buttons)b.setOnClickListener(v->{target.setColorIndex((Integer)v.getTag());d.dismiss();plot(false);}); d.show();
    }

    private void plot(boolean toast){
        double lo,hi; try{lo=num(minX.getText().toString());hi=num(maxX.getText().toString());if(!Double.isFinite(lo)||!Double.isFinite(hi)||lo>=hi||hi-lo>1e12)throw new Exception();}
        catch(Exception e){setStatus("Intervalo X inválido. O mínimo precisa ser menor que o máximo.",DANGER);if(toast)Toast.makeText(this,"Revise o eixo X.",Toast.LENGTH_SHORT).show();return;}
        List<GraphView.FunctionSpec> ok=new ArrayList<>(); boolean any=false,errors=false; int visible=0;
        for(FunctionRowView r:rows){r.clearError();String f=r.getFormula();if(f.isEmpty())continue;any=true;try{new Expression(f);ok.add(new GraphView.FunctionSpec(r.getNameValue(),f,GraphView.colorFor(r.getColorIndex()),r.isVisibleFunction()));if(r.isVisibleFunction())visible++;}catch(RuntimeException e){errors=true;r.showError(error(e.getMessage()));}}
        if(!any){setStatus("Adicione pelo menos uma função.",MUTED);return;} if(ok.isEmpty()){setStatus("Nenhuma função válida. Veja o erro na função.",DANGER);return;}
        graph.setFunctions(ok,lo,hi);save(); if(errors)setStatus("Algumas funções têm erro; as válidas continuam no gráfico.",DANGER); else if(visible==0)setStatus("Todas as funções estão ocultas. Ative uma com ON.",MUTED); else setStatus(visible+(visible==1?" curva visível":" curvas visíveis")+" • toque no gráfico para medir",ACCENT);
    }
    private String error(String m){if(m==null||m.isEmpty())return "Expressão inválida.";String s=m.replace("Esperado ')'","Falta ')' para fechar a expressão").replace("Expressão incompleta","A expressão terminou antes do esperado");return s.endsWith(".")?s:s+".";}

    private void load(){
        minX.setText(prefs.getString("minX","-10")); maxX.setText(prefs.getString("maxX","10")); String saved=prefs.getString("functions_v101","");
        if(saved!=null&&!saved.isEmpty())for(String line:saved.split("\\n")){String[] p=line.split("\\|",-1);if(p.length==4)try{addRow(dec(p[0]),dec(p[1]),Integer.parseInt(p[2]),"1".equals(p[3]));}catch(Exception ignored){}}
        if(rows.isEmpty()){String old=prefs.getString("equations","");if(old!=null&&!old.trim().isEmpty())for(String f:old.split("\\n"))if(!f.trim().isEmpty())addRow("",f,nextColor++,true);}
        if(rows.isEmpty())addRow("","",nextColor++,true);
    }
    private void save(){
        if(prefs==null||minX==null)return;StringBuilder s=new StringBuilder();for(FunctionRowView r:rows){if(s.length()>0)s.append('\n');s.append(enc(r.getNameValue())).append('|').append(enc(r.getFormula())).append('|').append(r.getColorIndex()).append('|').append(r.isVisibleFunction()?'1':'0');}
        prefs.edit().putString("functions_v101",s.toString()).putString("minX",minX.getText().toString()).putString("maxX",maxX.getText().toString()).apply();
    }
    private String enc(String s){return Base64.encodeToString(s.getBytes(StandardCharsets.UTF_8),Base64.URL_SAFE|Base64.NO_WRAP);} private String dec(String s){return new String(Base64.decode(s,Base64.URL_SAFE|Base64.NO_WRAP),StandardCharsets.UTF_8);}

    private LinearLayout numberBox(String label,EditText e){LinearLayout b=column();b.setPadding(dp(12),dp(6),dp(10),dp(4));b.setBackground(round(PANEL,13,BORDER,1));b.addView(text(label,10,MUTED,true));b.addView(e,new LinearLayout.LayoutParams(-1,dp(32)));return b;}
    private EditText number(){EditText e=new EditText(this);e.setSingleLine();e.setTextColor(TEXT);e.setTextSize(16);e.setPadding(0,0,0,0);e.setBackgroundColor(Color.TRANSPARENT);e.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_NUMBER_FLAG_SIGNED);return e;}
    private TextView section(String s){TextView t=text(s,11,MUTED,true);t.setLetterSpacing(.12f);t.setPadding(dp(2),0,0,dp(7));return t;}
    private TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextColor(color);t.setTextSize(size);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button primary(String s){Button b=button(s);b.setTextColor(Color.rgb(10,13,20));b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackground(round(ACCENT,14,0,0));return b;} private Button secondary(String s){Button b=button(s);b.setTextColor(TEXT);b.setBackground(round(PANEL2,14,BORDER,1));return b;}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setMinHeight(0);b.setMinWidth(0);return b;}
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;} private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);return l;}
    private GradientDrawable round(int fill,int radius,int stroke,int width){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(radius));if(width>0)d.setStroke(dp(width),stroke);return d;}
    private void setStatus(String s,int c){status.setText(s);status.setTextColor(c);} private double num(String s){return Double.parseDouble(s.trim().replace(',','.'));} private int dp(float v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
