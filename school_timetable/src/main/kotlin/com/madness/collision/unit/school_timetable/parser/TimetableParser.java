package com.madness.collision.unit.school_timetable.parser;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.text.HtmlCompat;

import com.madness.collision.R;
import com.madness.collision.unit.school_timetable.data.Timetable;
import com.madness.collision.util.CollisionDialog;
import com.madness.collision.util.X;

import java.util.HashMap;


class TimetableParser {

    static final String ERROR_NO_CLIP_DATA = "ERROR_NO_CLIP_DATA";
    static final String ERROR_NO_HTML = "ERROR_NO_HTML";
    static final String ERROR_NO_CONTENT = "ERROR_NO_CONTENT";
    static final String ERROR_UNDEFINED = "ERROR_UNDEFINED";

    /**
     * Get raw html content from clipboard
     */
    static String getRawFromClipboard(Context context){
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null || !clipboard.hasPrimaryClip()) return ERROR_NO_CLIP_DATA;
        ClipDescription desc = clipboard.getPrimaryClipDescription();
        if (desc == null) return ERROR_NO_CLIP_DATA;
        if (desc.hasMimeType("text/html")) {
            StringBuilder reBuilder = new StringBuilder();
            ClipData clipData = clipboard.getPrimaryClip();
            if (clipData == null) return ERROR_NO_CLIP_DATA;
            for (int i = 0; i < clipData.getItemCount(); i++) {
                reBuilder.append(clipData.getItemAt(0).getHtmlText());
            }
            return reBuilder.toString();
        } else {
            return ERROR_NO_HTML;
        }
    }

    /**
     * Parse raw html table and get standard content from it
     */
    static ParseResult parseRawTable(String rawTable){
        char template = 'a';
        if (rawTable.isEmpty()) return new ParseResult(template, ERROR_NO_CONTENT);
        String re = rawTable;

        // careful it's not a space but something stranger
        re = re.replace(" </td>", "?#null</td>");
        re = re.replace(" </td>", "?#null</td>");

        // 表格的一个格子结束后以?#terminal标记，便于灵活适配不同排版
        re = re.replace("</td>", "?#break?#terminal?#break</td>");
        re = re.replace("</th>", "?#break?#terminal?#break</th>");//template2

        // 表格的一行结束后以?#wrapTOE\n?#wrapHEAD标记，便于以表格行为整体
        re = re.replace("</td></tr>",
                "?#wrapTOE?#break?#wrapHEAD?#break</td></tr>");
        re = re.replace("</th></tr>",
                "?#wrapTOE?#break?#wrapHEAD?#break</th></tr>");//template2

        // 格子中空段落/空行以?#empty标记
        // 对调课、一个格子多节课等情况进行标记
        re = re.replace("</font><br", "</font>?#nothing<br");
        re = re.replace("><br", ">?#break?#empty<br");
        re = re.replace("?#nothing", "");

        // 一个格子内含段落大于一时以?#filled标记，便于筛选出只含空课的表格行
        re = re.replace("<br", "?#break?#filled<br");

        // 占两行或三行的格子以?#period2/?#period3标记，便于适配三节小课、一节小课情况
        re = re.replace("</td><td align=\"Center\" rowspan=\"2\"", "?#period2?#break</td><td align=\"Center\" rowspan=\"2\"");
        re = re.replace("</td><td align=\"Center\" rowspan=\"3\"", "?#period3?#break</td><td align=\"Center\" rowspan=\"3\"");
        re = re.replace("</td><td align=\"Center\" style=\"", "?#period1?#break</td><td align=\"Center\" style=\"");


        re = re.replace("</td><td align=\"Center\" class=\"noprint\" rowspan=\"2\"", "?#period2?#break</td><td align=\"Center\" rowspan=\"2\"");
        re = re.replace("</td><td align=\"Center\" class=\"noprint\" rowspan=\"3\"", "?#period3?#break</td><td align=\"Center\" rowspan=\"3\"");
        re = re.replace("</td><td align=\"Center\" class=\"noprint\" style=\"", "?#period1?#break</td><td align=\"Center\" style=\"");
        //template c
        re = re.replace("</th><td align=\"Center\" style=\"", "?#period1?#break</td><td align=\"Center\" style=\"");


        re = re.replace("</td><td align=\"center\" class=\"noprint\" rowspan=\"2\"", "?#period2?#break</td><td align=\"Center\" rowspan=\"2\"");
        re = re.replace("</td><td align=\"center\" class=\"noprint\" rowspan=\"3\"", "?#period3?#break</td><td align=\"Center\" rowspan=\"3\"");
        re = re.replace("</td><td align=\"center\" class=\"noprint\" style=\"", "?#period1?#break</td><td align=\"Center\" style=\"");

        re = re.replace("</td><td align=\"center\" rowspan=\"2\"", "?#period2?#break</td><td align=\"Center\" rowspan=\"2\"");
        re = re.replace("</td><td align=\"center\" rowspan=\"3\"", "?#period3?#break</td><td align=\"Center\" rowspan=\"3\"");
        re = re.replace("</td><td align=\"center\" style=\"", "?#period1?#break</td><td align=\"Center\" style=\"");
        //template c
        re = re.replace("</th><td align=\"center\" style=\"", "?#period1?#break</td><td align=\"Center\" style=\"");

        // 将调整过的HTML文本转换成字符串型
        re = HtmlCompat.fromHtml(re, HtmlCompat.FROM_HTML_MODE_COMPACT).toString();

        if (re.contains("╲"))
            template = 'c';
        if (re.contains("节次"))
            template = 'd';
        // 字符串中的‘%’应替换为‘%%’
        re = re.replace("%", "%%");
        // 将潜在存在的奇怪空格字符转换为正规空格字符
        re = re.replace(" ", " ");
        // 将?#break替换为换行符
        re = re.replace("?#break", "\n");
        if (template == 'd') {
            re = re.replaceAll("\n\\?#filled\n\\d{1,2}:\\d{2}-\\d{1,2}:\\d{2}", "?#todo\n?#terminal");
        }
        // 去除 时间 星期 早晨 上午 下午 晚上
        try {
            re = re.replace(re.substring(0, re.indexOf('午') - 1), "?#wrapHEAD\n");
        } catch (IndexOutOfBoundsException e) {
            try {
                re = re.replace(re.substring(0, re.indexOf('日') + 23), "");
            } catch (IndexOutOfBoundsException d) {
                return new ParseResult(template, ERROR_UNDEFINED);
            }
        }
        if (template == 'd') {
            //template d
            re = re.replace("1?#todo", "?#initiate1");
            re = re.replace("2?#todo", "?#initiate2");
            re = re.replace("3?#todo", "?#initiate3");
            re = re.replace("4?#todo", "?#initiate4");
            re = re.replace("5?#todo", "?#initiate5");
            re = re.replace("6?#todo", "?#initiate6");
            re = re.replace("7?#todo", "?#initiate7");
            re = re.replace("8?#todo", "?#initiate8");
            re = re.replace("9?#todo", "?#initiate9");
            re = re.replace("10?#todo", "?#initiate10");
            re = re.replace("11?#todo", "?#initiate11");
            re = re.replace("12?#todo", "?#initiate12");
            re = re.replace("13?#todo", "?#initiate13");
            re = re.replace("14?#todo", "?#initiate14");
        } else {
            /*
            将课时替换为自定义字符
             */
            re = re.replace("第1节\n?#terminal", "?#initiate1");
            re = re.replace("第2节\n?#terminal", "?#initiate2");
            re = re.replace("第3节\n?#terminal", "?#initiate3");
            re = re.replace("第4节\n?#terminal", "?#initiate4");
            re = re.replace("第5节\n?#terminal", "?#initiate5");
            re = re.replace("第6节\n?#terminal", "?#initiate6");
            re = re.replace("第7节\n?#terminal", "?#initiate7");
            re = re.replace("第8节\n?#terminal", "?#initiate8");
            re = re.replace("第9节\n?#terminal", "?#initiate9");
            re = re.replace("第10节\n?#terminal", "?#initiate10");
            re = re.replace("第11节\n?#terminal", "?#initiate11");
            re = re.replace("第12节\n?#terminal", "?#initiate12");
            re = re.replace("第13节\n?#terminal", "?#initiate13");
            re = re.replace("第14节\n?#terminal", "?#initiate14");
            re = re.replace("第一节\n?#terminal", "?#initiate1");
            re = re.replace("第二节\n?#terminal", "?#initiate2");
            re = re.replace("第三节\n?#terminal", "?#initiate3");
            re = re.replace("第四节\n?#terminal", "?#initiate4");
            re = re.replace("第五节\n?#terminal", "?#initiate5");
            re = re.replace("第六节\n?#terminal", "?#initiate6");
            re = re.replace("第七节\n?#terminal", "?#initiate7");
            re = re.replace("第八节\n?#terminal", "?#initiate8");
            re = re.replace("第九节\n?#terminal", "?#initiate9");
            re = re.replace("第十节\n?#terminal", "?#initiate10");
            re = re.replace("第十一节\n?#terminal", "?#initiate11");
            re = re.replace("第十二节\n?#terminal", "?#initiate12");
            re = re.replace("第十三节\n?#terminal", "?#initiate13");
            re = re.replace("第十四节\n?#terminal", "?#initiate14");
            //template2
            re = re.replace("1,2节\n?#terminal", "?#morning\n?#initiate1");
            re = re.replace("3,4节\n?#terminal", "?#initiate3");
            re = re.replace("5,6节\n?#terminal", "?#afternoon\n?#initiate5");
            re = re.replace("7,8节\n?#terminal", "?#initiate7");
            re = re.replace("备注\n?#terminal", "?#description\n?#filled");
            re = re.replace("晚 上\n?#terminal", "?#evening");
        }
        re = re.replace("上午\n?#terminal", "?#morning");
        re = re.replace("下午\n?#terminal", "?#afternoon");
        re = re.replace("晚上\n?#terminal", "?#evening");
            /*
            replace potential <br/> with \n
             */
        re = re.replace("<br/>", "\n");
        re += "\n?#wrapTOE";//template d needs it

        String[] contentParts;
        contentParts = re.split(re.contains("<br/>") ? "<br/>" : "\n");
        StringBuilder union = new StringBuilder();
        StringBuilder regain = new StringBuilder();
        boolean vacancy = true;
        for (String s : contentParts) {
            union.append(s);
            if (union.toString().endsWith("?#filled") && vacancy)
                vacancy = false;
            if (union.toString().endsWith("?#wrapTOE")) {
                if (!vacancy) {
                    regain.append(union);
                    vacancy = true;
                }
                union = new StringBuilder();
            }
            union.append("\n");
        }
        return new ParseResult(template, regain.toString());
    }

    static HashMap<String, String> resolveDescription(String desc){
        HashMap<String, String> descriptionMap = new HashMap<>();
        String description = desc.substring(desc.indexOf("?#filled\n") + 9, desc.indexOf("?#terminal") - 1);
        String[] descriptionParts = description.split("；");
        for (String s : descriptionParts) {
            descriptionMap.put(s.substring(0, s.indexOf(":")), s.substring(s.indexOf(":") + 1));
        }
        return descriptionMap;
    }

    /**
     * Potential broken information
     */
    static ParseResult confirmStandard(Context context, String standard){
        String[] processParts = standard.split("\n");
        boolean implementInput = false;
        int cycle = 0;
        String testString;
        StringBuilder preString = null;
        String farPreString = null;
        String[] indexSummary = new String[10];
        for (String text: processParts){
            testString = text;
            if (testString.contains("节/周") && !testString.contains("{")){
                implementInput = true;
                testString = farPreString + "\n" + testString.replace("{", "?#l").replace("}", "?#r") + "\n";
                if ((cycle > 0 && !testString.matches(indexSummary[cycle - 1])) || cycle == 0){
                    indexSummary[cycle] = testString;
                    cycle ++;
                }
            }
            if (preString != null) farPreString = preString.toString();
            preString = new StringBuilder(testString);
        }
        preString = new StringBuilder();
        if (implementInput){
            for(cycle = 0; indexSummary[cycle] != null; cycle ++) {
                preString.append(indexSummary[cycle]);
                preString.append("\n");
            }
            preString = new StringBuilder(preString.toString().replace("?#l", "{").replace("?#r", "}"));
            preString.append(context.getString(com.madness.collision.unit.school_timetable.R.string.ics_Function_AlertDialog_Instruction));
        }
        return new ParseResult(implementInput ? '1' : '0', preString.toString());
    }

    interface InputCallback{
        void getInput(int startWeek, int endWeek);
    }

    static void getInput(Context context, String preString, InputCallback callback){
        CollisionDialog dialogPause = new CollisionDialog(context, R.string.text_forgetit, R.string.text_allset, true);
        dialogPause.setTitleCollision(com.madness.collision.unit.school_timetable.R.string.ics_Function_AlertDialog_Title, 0, 0);
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        TextView tv = new TextView(context);
        tv.setText(preString);
        layout.addView(tv);
        final EditText editTextStartWeek = new EditText(context);
        editTextStartWeek.setHint(com.madness.collision.unit.school_timetable.R.string.ics_Function_AlertDialog_EditText_StartingWeekHint);
        editTextStartWeek.setInputType(InputType.TYPE_CLASS_NUMBER);
        editTextStartWeek.setBackgroundColor(Color.TRANSPARENT);
        layout.addView(editTextStartWeek);
        final EditText editTextEndWeek = new EditText(context);
        editTextEndWeek.setHint(com.madness.collision.unit.school_timetable.R.string.ics_Function_AlertDialog_EditText_EndingWeekHint);
        editTextEndWeek.setInputType(InputType.TYPE_CLASS_NUMBER);
        editTextEndWeek.setBackgroundColor(Color.TRANSPARENT);
        layout.addView(editTextEndWeek);
        int dp10 = (int) X.INSTANCE.size(context, 10, X.DP);
        layout.setPadding(dp10 * 2, dp10, dp10 * 2, dp10);
        dialogPause.setCustomContent(layout);
        dialogPause.setContent(0);
        dialogPause.setListener(v -> dialogPause.dismiss(), v -> {
            dialogPause.dismiss();
            try {
                int startWeek = Integer.parseInt(editTextStartWeek.getText().toString());
                int endWeek = Integer.parseInt(editTextEndWeek.getText().toString());
                callback.getInput(startWeek, endWeek);
            }catch (NumberFormatException e){
                e.printStackTrace();
                X.INSTANCE.toast(context, R.string.res_invalid_input, Toast.LENGTH_LONG);
            }
        });
        dialogPause.show();
    }

    /**
     * Parse standard content
     */
    static Timetable resolveStandard(Context context, ParseResult re){
        StandardParser standardParser = new StandardParser(context);
        standardParser.parse(re);
        return standardParser.timetable;
    }
}
