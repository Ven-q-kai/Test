package com.venqkai.graphlab;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class MainActivity110 extends Activity implements FunctionRowView.Listener, MathKeyboardView.Listener {
    private static final int BG=Color.rgb(10,12,17), SURFACE=Color.rgb(17,20,28), SURFACE2=Color.rgb(23,27,37);
    private static final int TEXT=Color.rgb(240,243,249), MUTED=Color.rgb(145,154,174), BORDER=Color.rgb(42,49,64);
    private static final int ACCENT=Color.rgb(132,165,255), DANGER=Color.rgb(255,112,99), GOOD=Color.rgb(92,214,145);

    private final List<FunctionRowView> rows=new ArrayList<>();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable replot=()->plot(false);
    private SharedPreferences prefs;
    private GraphView graph;
    private LinearLayout root, header, toolbar, rowBox;
    private ScrollView functionScroll;
    private TextView status;
    private FunctionRowView activeRow;
    private EditText minX,maxX,minY,maxY;
    private boolean autoY=true, fullscreen=false, loading=true;
    private int nextColor=0;

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        Window w=getWindow(); w.setStatusBarColor(BG); w.setNavigationBarColor(BG);
        prefs=getSharedPreferences("graphlab",MODE_PRIVATE);
        setContentView(buildUi()); load(); loading=false; plot(false);
    }

    @Override protected void onPause(){ super.onPause(); save(); }
    @Override public void onBackPressed(){ if(fullscreen) toggleFullscreen(); else super.onBackPressed(); }

    private View buildUi(){
        root=column(); root.setBackgroundColor(BG); root.setPadding(dp(14),dp(8),dp(14),dp(12));

        header=row(); header.setGravity(Gravity.CENTER_VERTICAL); header.setPadding(dp(2),dp(2),dp(2),dp(8));
        LinearLayout titles=column(); TextView title=text("GraphLab",25,TEXT,true); TextView sub=text("gráficos e análise matemática",12,MUTED,false);
        titles.addView(title); titles.addView(sub); header.addView(titles,new LinearLayout.LayoutParams(0,-2,1));
        TextView version=text("1.1.0",11,ACCENT,true); version.setGravity(Gravity.CENTER); version.setPadding(dp(10),dp(5),dp(10),dp(5)); version.setBackground(round(Color.rgb(25,33,52),30,Color.rgb(53,70,105),1)); header.addView(version);
        Button fs=iconButton("⛶","Tela cheia"); fs.setOnClickListener(v->toggleFullscreen()); LinearLayout.LayoutParams fsp=new LinearLayout.LayoutParams(dp(42),dp(38)); fsp.leftMargin=dp(7); header.addView(fs,fsp);
        root.addView(header);

        graph=new GraphView(this); graph.setBackground(round(Color.rgb(13,16,23),20,BORDER,1));
        LinearLayout.LayoutParams gp=new LinearLayout.LayoutParams(-1,dp(390)); gp.bottomMargin=dp(8); root.addView(graph,gp);

        toolbar=row(); toolbar.setGravity(Gravity.CENTER_VERTICAL); toolbar.setPadding(0,0,0,dp(8));
        toolbar.addView(toolButton("＋","Função",v->addAndFocus()),weight());
        toolbar.addView(toolButton("⌨","Teclado",v->showKeyboard()),weightWithMargins());
        toolbar.addView(toolButton("▦","Tabela",v->showTable()),weightWithMargins());
        toolbar.addView(toolButton("∑","Analisar",v->showAnalysis()),weightWithMargins());
        toolbar.addView(toolButton("⚙","Eixos",v->showSettings()),weightWithMargins());
        root.addView(toolbar);

        functionScroll=new ScrollView(this); functionScroll.setFillViewport(false); root.addView(functionScroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout body=column(); body.setPadding(0,dp(2),0,dp(10)); functionScroll.addView(body,new ScrollView.LayoutParams(-1,-2));
        LinearLayout sectionHead=row(); sectionHead.setGravity(Gravity.CENTER_VERTICAL); sectionHead.addView(text("FUNÇÕES",11,MUTED,true),new LinearLayout.LayoutParams(0,-2,1));
        TextView live=text("● ao vivo",11,GOOD,false); sectionHead.addView(live); body.addView(sectionHead);
        rowBox=column(); LinearLayout.LayoutParams rbp=new LinearLayout.LayoutParams(-1,-2); rbp.topMargin=dp(7); body.addView(rowBox,rbp);

        Button add=secondary("＋ Adicionar função"); add.setOnClickListener(v->addAndFocus()); LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,dp(46)); ap.topMargin=dp(2); body.addView(add,ap);
        TextView syntax=text("Funções: sin, cos, tan, asin, acos, atan, sinh, cosh, tanh, sqrt, cbrt, abs, ln, log, exp, floor, ceil, round, sign, deg, rad, min, max, pow, root e atan2.",11,MUTED,false); syntax.setPadding(dp(3),dp(10),dp(3),dp(4)); body.addView(syntax);

        status=text("Pronto.",11,MUTED,false); status.setGravity(Gravity.CENTER); status.setPadding(dp(4),dp(7),dp(4),0); root.addView(status);

        minX=hiddenNumber("-10"); maxX=hiddenNumber("10"); minY=hiddenNumber("-10"); maxY=hiddenNumber("10");
        return root;
    }

    private void addAndFocus(){ addRow("","",nextColor++,true); activeRow=rows.get(rows.size()-1); activeRow.requestFormulaFocus(); }

    private void addRow(String name,String formula,int color,boolean visible){
        FunctionRowView r=new FunctionRowView(this,name,formula,color,visible,this); rows.add(r); nextColor=Math.max(nextColor,color+1);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.bottomMargin=dp(8); rowBox.addView(r,p); if(activeRow==null)activeRow=r;
    }

    @Override public void onChanged(){ schedule(); }
    @Override public void onDelete(FunctionRowView r){ if(rows.size()==1){r.clearFields();activeRow=r;schedule();return;} rows.remove(r);rowBox.removeView(r);if(activeRow==r)activeRow=rows.get(0);schedule(); }
    @Override public void onDuplicate(FunctionRowView r){ addRow(r.getNameValue(),r.getFormula(),nextColor++,r.isVisibleFunction());activeRow=rows.get(rows.size()-1);activeRow.requestFormulaFocus();schedule(); }
    @Override public void onColorRequested(FunctionRowView r){ colorDialog(r); }
    @Override public void onFormulaFocused(FunctionRowView r){ activeRow=r; }
    @Override public void onInsert(String value,int cursorBack){ if(activeRow==null&&!rows.isEmpty())activeRow=rows.get(0); if(activeRow!=null)activeRow.insertIntoFormula(value,cursorBack); }

    private void schedule(){ if(loading)return;handler.removeCallbacks(replot);handler.postDelayed(replot,320); }

    private void plot(boolean noisy){
        handler.removeCallbacks(replot); double lo,hi,ylo=-10,yhi=10;
        try{lo=num(minX);hi=num(maxX);if(!validRange(lo,hi))throw new Exception();}
        catch(Exception e){setStatus("Eixo X inválido",DANGER);return;}
        if(!autoY){try{ylo=num(minY);yhi=num(maxY);if(!validRange(ylo,yhi))throw new Exception();}catch(Exception e){setStatus("Eixo Y inválido",DANGER);return;}}

        List<GraphView.FunctionSpec> specs=new ArrayList<>(); int visible=0; boolean any=false,errors=false;
        for(FunctionRowView r:rows){ r.clearError(); String f=r.getFormula(); if(f.isEmpty())continue; any=true; try{new Expression(f); specs.add(new GraphView.FunctionSpec(r.getNameValue(),f,GraphView.colorFor(r.getColorIndex()),r.isVisibleFunction())); if(r.isVisibleFunction())visible++;}catch(RuntimeException e){errors=true;r.showError(humanError(e.getMessage()));}}
        if(!any){setStatus("Adicione uma função",MUTED);return;} if(specs.isEmpty()){setStatus("Nenhuma função válida",DANGER);return;}
        graph.setFunctions(specs,lo,hi,autoY,ylo,yhi); save();
        if(errors)setStatus("Algumas expressões têm erro",DANGER); else setStatus(visible+(visible==1?" curva visível":" curvas visíveis"),ACCENT);
        if(noisy)Toast.makeText(this,"Gráfico atualizado",Toast.LENGTH_SHORT).show();
    }

    private List<AnalysisEngine.FunctionInput> visibleInputs(){
        List<AnalysisEngine.FunctionInput> list=new ArrayList<>();
        for(FunctionRowView r:rows) if(r.isVisibleFunction()&&!r.getFormula().isEmpty()) try{new Expression(r.getFormula());list.add(new AnalysisEngine.FunctionInput(r.getNameValue(),r.getFormula(),GraphView.colorFor(r.getColorIndex())));}catch(RuntimeException ignored){}
        return list;
    }

    private void showAnalysis(){
        List<AnalysisEngine.FunctionInput> list=visibleInputs(); if(list.isEmpty()){Toast.makeText(this,"Ative pelo menos uma função válida.",Toast.LENGTH_SHORT).show();return;}
        double lo,hi; try{lo=num(minX);hi=num(maxX);}catch(Exception e){return;}
        AnalysisEngine.Report report=AnalysisEngine.analyze(list,lo,hi);
        ScrollView scroll=new ScrollView(this); LinearLayout box=column(); box.setPadding(dp(18),dp(8),dp(18),dp(18)); scroll.addView(box);
        box.addView(dialogLead("Análise numérica no intervalo X atual"));
        for(AnalysisEngine.FunctionReport fr:report.functions){
            box.addView(dialogTitle(fr.input.name));
            box.addView(dialogLine("Raízes",formatValues(fr.roots)));
            box.addView(dialogLine("Mínimos",formatPoints(fr.minima)));
            box.addView(dialogLine("Máximos",formatPoints(fr.maxima)));
            box.addView(dialogLine("Integral ∫",AnalysisEngine.format(fr.integral)));
            box.addView(dialogLine("Derivada em x="+AnalysisEngine.format(fr.centerX),AnalysisEngine.format(fr.derivativeAtCenter)));
        }
        if(report.intersections.isEmpty()) box.addView(dialogLine("Interseções","nenhuma detectada"));
        else{ box.addView(dialogTitle("Interseções")); for(AnalysisEngine.Intersection i:report.intersections) box.addView(dialogLine(i.a+" × "+i.b,"("+AnalysisEngine.format(i.point.x)+", "+AnalysisEngine.format(i.point.y)+")")); }
        TextView note=text("Resultados são aproximações numéricas. Descontinuidades e funções muito oscilatórias podem exigir um intervalo menor.",11,MUTED,false); note.setPadding(0,dp(12),0,0); box.addView(note);
        new AlertDialog.Builder(this).setTitle("Analisar").setView(scroll).setPositiveButton("Fechar",null).show();
    }

    private void showTable(){
        List<AnalysisEngine.FunctionInput> inputs=visibleInputs(); if(inputs.isEmpty()){Toast.makeText(this,"Ative pelo menos uma função válida.",Toast.LENGTH_SHORT).show();return;}
        double lo,hi; try{lo=num(minX);hi=num(maxX);}catch(Exception e){return;}
        HorizontalScrollView hscroll=new HorizontalScrollView(this); ScrollView vscroll=new ScrollView(this); LinearLayout table=column(); table.setPadding(dp(16),dp(10),dp(16),dp(16)); vscroll.addView(table); hscroll.addView(vscroll);
        StringBuilder head=new StringBuilder("x"); for(AnalysisEngine.FunctionInput i:inputs)head.append("      ").append(shortName(i.name));
        TextView hv=text(head.toString(),13,TEXT,true); hv.setTypeface(Typeface.MONOSPACE,Typeface.BOLD); table.addView(hv);
        int count=13; List<Expression> exprs=new ArrayList<>(); for(AnalysisEngine.FunctionInput i:inputs)exprs.add(new Expression(i.formula));
        for(int row=0;row<count;row++){ double x=lo+(hi-lo)*row/(count-1.0); StringBuilder line=new StringBuilder(pad(AnalysisEngine.format(x),10)); for(Expression e:exprs){double y;try{y=e.eval(x);}catch(Exception ex){y=Double.NaN;}line.append(pad(AnalysisEngine.format(y),14));} TextView tv=text(line.toString(),12,Color.rgb(215,220,232),false);tv.setTypeface(Typeface.MONOSPACE);tv.setPadding(0,dp(5),0,dp(5));table.addView(tv); }
        new AlertDialog.Builder(this).setTitle("Tabela de valores").setView(hscroll).setPositiveButton("Fechar",null).show();
    }

    private void showKeyboard(){
        LinearLayout wrap=column(); wrap.setPadding(dp(12),dp(8),dp(12),dp(8)); wrap.addView(new MathKeyboardView(this,this));
        new AlertDialog.Builder(this).setTitle("Teclado matemático").setView(wrap).setNegativeButton("Fechar",null).show();
    }

    private void showSettings(){
        LinearLayout box=column(); box.setPadding(dp(18),dp(5),dp(18),dp(8));
        box.addView(dialogLead("Intervalos do gráfico"));
        EditText x1=settingInput(minX.getText().toString()),x2=settingInput(maxX.getText().toString()),y1=settingInput(minY.getText().toString()),y2=settingInput(maxY.getText().toString());
        box.addView(pair("X mínimo",x1,"X máximo",x2));
        LinearLayout autoRow=row();autoRow.setGravity(Gravity.CENTER_VERTICAL);TextView label=text("Eixo Y automático",14,TEXT,true);autoRow.addView(label,new LinearLayout.LayoutParams(0,-2,1));Button auto=secondary(autoY?"ATIVO":"MANUAL");auto.setOnClickListener(v->{autoY=!autoY;auto.setText(autoY?"ATIVO":"MANUAL");y1.setEnabled(!autoY);y2.setEnabled(!autoY);});autoRow.addView(auto,new LinearLayout.LayoutParams(dp(100),dp(40)));LinearLayout.LayoutParams arp=new LinearLayout.LayoutParams(-1,-2);arp.topMargin=dp(12);box.addView(autoRow,arp);
        y1.setEnabled(!autoY);y2.setEnabled(!autoY);box.addView(pair("Y mínimo",y1,"Y máximo",y2));
        AlertDialog d=new AlertDialog.Builder(this).setTitle("Eixos e enquadramento").setView(box).setNegativeButton("Cancelar",null).setPositiveButton("Aplicar",null).create();
        d.setOnShowListener(v->d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(b->{try{double a=parse(x1),c=parse(x2);if(!validRange(a,c))throw new Exception();if(!autoY){double q=parse(y1),z=parse(y2);if(!validRange(q,z))throw new Exception();}minX.setText(x1.getText());maxX.setText(x2.getText());minY.setText(y1.getText());maxY.setText(y2.getText());d.dismiss();plot(false);}catch(Exception e){Toast.makeText(this,"Revise os limites dos eixos.",Toast.LENGTH_SHORT).show();}})); d.show();
    }

    private View pair(String la,EditText a,String lb,EditText b){LinearLayout r=row();LinearLayout aa=fieldBox(la,a),bb=fieldBox(lb,b);LinearLayout.LayoutParams p1=new LinearLayout.LayoutParams(0,dp(62),1);p1.rightMargin=dp(6);LinearLayout.LayoutParams p2=new LinearLayout.LayoutParams(0,dp(62),1);p2.leftMargin=dp(6);r.addView(aa,p1);r.addView(bb,p2);LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);rp.topMargin=dp(9);r.setLayoutParams(rp);return r;}

    private void colorDialog(FunctionRowView target){GridLayout g=new GridLayout(this);g.setColumnCount(4);g.setPadding(dp(14),dp(8),dp(14),dp(8));List<Button> bs=new ArrayList<>();for(int i=0;i<GraphView.paletteSize();i++){Button b=new Button(this);b.setTag(i);b.setMinWidth(0);b.setMinHeight(0);b.setBackground(round(GraphView.colorFor(i),100,i==target.getColorIndex()?Color.WHITE:Color.argb(50,255,255,255),i==target.getColorIndex()?2:1));GridLayout.LayoutParams p=new GridLayout.LayoutParams();p.width=dp(50);p.height=dp(50);p.setMargins(dp(6),dp(6),dp(6),dp(6));g.addView(b,p);bs.add(b);}AlertDialog d=new AlertDialog.Builder(this).setTitle("Cor da função").setView(g).setNegativeButton("Cancelar",null).create();for(Button b:bs)b.setOnClickListener(v->{target.setColorIndex((Integer)v.getTag());d.dismiss();plot(false);});d.show();}

    private void toggleFullscreen(){fullscreen=!fullscreen;header.setVisibility(fullscreen?View.GONE:View.VISIBLE);toolbar.setVisibility(fullscreen?View.GONE:View.VISIBLE);functionScroll.setVisibility(fullscreen?View.GONE:View.VISIBLE);status.setVisibility(fullscreen?View.GONE:View.VISIBLE);LinearLayout.LayoutParams p=(LinearLayout.LayoutParams)graph.getLayoutParams();p.height=fullscreen?0:dp(390);p.weight=fullscreen?1f:0f;graph.setLayoutParams(p);root.setPadding(fullscreen?0:dp(14),fullscreen?0:dp(8),fullscreen?0:dp(14),fullscreen?0:dp(12));}

    private void load(){minX.setText(prefs.getString("minX","-10"));maxX.setText(prefs.getString("maxX","10"));minY.setText(prefs.getString("minY","-10"));maxY.setText(prefs.getString("maxY","10"));autoY=prefs.getBoolean("autoY",true);String saved=prefs.getString("functions_v101","");if(saved!=null&&!saved.isEmpty())for(String line:saved.split("\\n")){String[] p=line.split("\\|",-1);if(p.length==4)try{addRow(dec(p[0]),dec(p[1]),Integer.parseInt(p[2]),"1".equals(p[3]));}catch(Exception ignored){}}if(rows.isEmpty())addRow("","",nextColor++,true);}
    private void save(){if(prefs==null)return;StringBuilder s=new StringBuilder();for(FunctionRowView r:rows){if(s.length()>0)s.append('\n');s.append(enc(r.getNameValue())).append('|').append(enc(r.getFormula())).append('|').append(r.getColorIndex()).append('|').append(r.isVisibleFunction()?'1':'0');}prefs.edit().putString("functions_v101",s.toString()).putString("minX",minX.getText().toString()).putString("maxX",maxX.getText().toString()).putString("minY",minY.getText().toString()).putString("maxY",maxY.getText().toString()).putBoolean("autoY",autoY).apply();}

    private String formatValues(List<Double> xs){if(xs.isEmpty())return "nenhuma detectada";StringBuilder s=new StringBuilder();for(int i=0;i<xs.size();i++){if(i>0)s.append(", ");s.append(AnalysisEngine.format(xs.get(i)));}return s.toString();}
    private String formatPoints(List<AnalysisEngine.Point> ps){if(ps.isEmpty())return "nenhum detectado";StringBuilder s=new StringBuilder();for(int i=0;i<ps.size();i++){if(i>0)s.append("  ");AnalysisEngine.Point p=ps.get(i);s.append('(').append(AnalysisEngine.format(p.x)).append(", ").append(AnalysisEngine.format(p.y)).append(')');}return s.toString();}
    private TextView dialogLead(String s){TextView t=text(s,12,MUTED,false);t.setPadding(0,0,0,dp(10));return t;}
    private TextView dialogTitle(String s){TextView t=text(s,15,TEXT,true);t.setPadding(0,dp(12),0,dp(5));return t;}
    private TextView dialogLine(String a,String b){TextView t=text(a+"\n"+b,12,Color.rgb(215,220,232),false);t.setPadding(dp(10),dp(8),dp(10),dp(8));t.setBackground(round(SURFACE2,10,BORDER,1));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(5);t.setLayoutParams(p);return t;}

    private Button toolButton(String icon,String label,View.OnClickListener c){Button b=secondary(icon+"\n"+label);b.setTextSize(11);b.setGravity(Gravity.CENTER);b.setOnClickListener(c);return b;}
    private LinearLayout.LayoutParams weight(){return new LinearLayout.LayoutParams(0,dp(54),1);}
    private LinearLayout.LayoutParams weightWithMargins(){LinearLayout.LayoutParams p=weight();p.leftMargin=dp(5);return p;}
    private Button iconButton(String s,String desc){Button b=secondary(s);b.setContentDescription(desc);b.setTextSize(18);return b;}
    private Button secondary(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(TEXT);b.setTextSize(13);b.setMinWidth(0);b.setMinHeight(0);b.setPadding(dp(6),0,dp(6),0);b.setBackground(round(SURFACE2,13,BORDER,1));return b;}
    private EditText hiddenNumber(String s){EditText e=settingInput(s);e.setVisibility(View.GONE);return e;}
    private EditText settingInput(String s){EditText e=new EditText(this);e.setSingleLine();e.setText(s);e.setTextColor(TEXT);e.setHintTextColor(MUTED);e.setTextSize(15);e.setPadding(0,0,0,0);e.setBackgroundColor(Color.TRANSPARENT);e.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_NUMBER_FLAG_SIGNED);return e;}
    private LinearLayout fieldBox(String label,EditText e){LinearLayout b=column();b.setPadding(dp(10),dp(5),dp(10),dp(3));b.setBackground(round(SURFACE2,12,BORDER,1));b.addView(text(label,10,MUTED,true));b.addView(e,new LinearLayout.LayoutParams(-1,dp(34)));return b;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);return l;} private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private TextView text(String s,int sp,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private GradientDrawable round(int fill,int radius,int stroke,int width){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(radius));if(width>0)d.setStroke(dp(width),stroke);return d;}
    private void setStatus(String s,int c){status.setText(s);status.setTextColor(c);} private boolean validRange(double a,double b){return Double.isFinite(a)&&Double.isFinite(b)&&a<b&&b-a>=1e-10&&b-a<=1e12;} private double num(EditText e){return parse(e);} private double parse(EditText e){return Double.parseDouble(e.getText().toString().trim().replace(',','.'));}
    private String humanError(String m){if(m==null||m.isEmpty())return "Expressão inválida.";return m.endsWith(".")?m:m+".";} private String enc(String s){return Base64.encodeToString(s.getBytes(StandardCharsets.UTF_8),Base64.URL_SAFE|Base64.NO_WRAP);} private String dec(String s){return new String(Base64.decode(s,Base64.URL_SAFE|Base64.NO_WRAP),StandardCharsets.UTF_8);} private String shortName(String s){if(s==null||s.isEmpty())return "f(x)";return s.length()>10?s.substring(0,9)+"…":s;} private String pad(String s,int n){StringBuilder b=new StringBuilder(s);while(b.length()<n)b.append(' ');return b.toString();} private int dp(float v){return Math.round(v*getResources().getDisplayMetrics().density);}
}