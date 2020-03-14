package com.madness.collision.unit.school_timetable.parser;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;

import com.madness.collision.unit.school_timetable.data.TimetablePeriod;
import com.madness.collision.util.F;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Get raw HTML table from raw HTML
 */
class TableParser {
    private SparseIntArray columnsName;
    private SparseArray<TimetablePeriod> rowsName; // rowsName: 1..14, -5..-1  columnsName: 0..6
    private String legacyContent = "";
    private boolean legacySuccessfullyConstructed;
    private boolean legacyUnsupportedBrowser;

    boolean isLegacyUnsupportedBrowser() {
        return legacyUnsupportedBrowser;
    }

    boolean isLegacySuccessfullyConstructed() {
        return legacySuccessfullyConstructed;
    }

    String getLegacyContent() {
        return legacyContent;
    }

    TableParser(Context context){
        String re = TimetableParser.getRawFromClipboard(context);
        legacyUnsupportedBrowser = re.equals(TimetableParser.ERROR_NO_HTML);
        legacySuccessfullyConstructed = !re.isEmpty() && !legacyUnsupportedBrowser && !re.equals(TimetableParser.ERROR_NO_CLIP_DATA);
        if (legacySuccessfullyConstructed) constructTimetable(context, re);
    }

    TableParser(Context context, String content){
        constructTimetable(context, content);
    }

    private void constructTimetable(Context context, String ttContent){
        legacySuccessfullyConstructed = true;
        storeTimetable(context, ttContent);
        rowsName = new SparseArray<>();
        columnsName = new SparseIntArray();
        for (Element table : getTables(ttContent)){
            iterateTable(table);
            if (rowsName.size() > 0 && columnsName.size() > 0){
                legacyContent = table.outerHtml().replaceAll("\n", "").replaceAll("> *<", "><");
                return;
            }
        }
    }

    private void storeTimetable(Context context, String ttContent){
        F f = F.INSTANCE;
        File file = new File(f.valFilePubTtCode(context));
        if (!f.prepare4(file)) return;
        try (FileOutputStream stream = new FileOutputStream(file)) {
            stream.write(ttContent.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private ArrayList<Element> getTables(String in){
        Document document = Jsoup.parse(in);
        ArrayList<Element> tables = new ArrayList<>(document.getElementsByTag("table"));
        Elements iFrames = document.getElementsByTag("iframe");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            iFrames.forEach(element -> tables.addAll(element.getElementsByTag(("table"))));
        }else {
            for (Element frame : iFrames){
                tables.addAll(frame.getElementsByTag(("table")));
            }
        }
        return tables;
    }

    private void iterateTable(Element table){
        //identify properties
        int rowIndex = 0, columnIndex;
        Rect region;
        for (Element body : table.children()){
            for (Element row : body.children()){
                columnIndex = 0;
                for (Element cell : row.children()){
                    Point coordinates = new Point(columnIndex, rowIndex);
                    region = new Rect(
                            coordinates.x, coordinates.y,
                            coordinates.x + Integer.valueOf(cell.attr("colspan").isEmpty() ? "1" : cell.attr("colspan")) - 1,
                            coordinates.y + Integer.valueOf(cell.attr("rowspan").isEmpty() ? "1" : cell.attr("rowspan")) - 1
                    );
                    addProperty(cell, coordinates);
                    columnIndex += region.right - region.left;
                    columnIndex ++;
                }
                rowIndex ++;
            }
        }
    }

    private void addProperty(Element in, Point coordinates){
        String text = in.text();
        Pattern pattern = Pattern.compile("((^第?((\\d,?\\d?)|(十?[一二三四五六七八九十]))节)|(^\\d\\d?))\\s?\\(?(\\d\\d?:\\d\\d-\\d\\d?:\\d\\d)?\\)?");
        Matcher matcher = pattern.matcher(text);
        if (matcher.matches()){
            pattern = Pattern.compile("\\d{1,2}:\\d{2}-\\d{1,2}:\\d{2}");
            matcher = matcher.usePattern(pattern);
            text = text.replaceAll(pattern.pattern(), "");
            while (matcher.find()){
                Log.d("Timetable time period", matcher.group());//time period
            }
            pattern = Pattern.compile("(\\d,?\\d?)|(十?[一二三四五六七八九十])|(^\\d{1,2})");
            matcher.reset(text);
            matcher.usePattern(pattern);
            while (matcher.find()){
                Matcher matcherInner;
                text = matcher.group();
                Log.d("Timetable class period", text);//class period
                pattern = Pattern.compile("\\d{1,2}");
                matcherInner = pattern.matcher(text);
                if (matcherInner.matches()){
                    Log.d("Timetable", "number");
                    try {
                        rowsName.append(coordinates.y, TimetablePeriod.Companion.parse(text));
                    }catch (IllegalArgumentException e){
                        e.printStackTrace();
                    }
                } else{
                    pattern = Pattern.compile("十?[一二三四五六七八九十]");
                    matcherInner.usePattern(pattern);
                    if (matcherInner.matches()){
                        Log.d("Timetable", "Chinese");
                        text = text.replace("一", "1")
                                .replace("二", "2")
                                .replace("三", "3")
                                .replace("四", "4")
                                .replace("五", "5")
                                .replace("六", "6")
                                .replace("七", "7")
                                .replace("八", "8")
                                .replace("九", "9");
                        if (text.equals("十"))
                            text = "10";
                        else
                            text = text.replace("十", "1");
                        try {
                            rowsName.append(coordinates.y, TimetablePeriod.Companion.parse(text));
                        }catch (IllegalArgumentException e){
                            e.printStackTrace();
                        }
                    }else {
                        pattern = Pattern.compile("\\d,?\\d?");
                        matcherInner.usePattern(pattern);
                        if (matcherInner.matches()){
                            Log.d("Timetable", "double");
                            try {
                                rowsName.append(coordinates.y, TimetablePeriod.Companion.parse(text));
                            }catch (IllegalArgumentException e){
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }else {
            switch (in.text()){
                case "时间":case "节次":case "节次/时间":case "╲":
                    break;
                case "星期日":
                    columnsName.append(coordinates.x, 0);
                    break;
                case "星期一":
                    columnsName.append(coordinates.x, 1);
                    break;
                case "星期二":
                    columnsName.append(coordinates.x, 2);
                    break;
                case "星期三":
                    columnsName.append(coordinates.x, 3);
                    break;
                case "星期四":
                    columnsName.append(coordinates.x, 4);
                    break;
                case "星期五":
                    columnsName.append(coordinates.x, 5);
                    break;
                case "星期六":
                    columnsName.append(coordinates.x, 6);
                    break;
//                case "早晨":
//                case "上午":
//                case "下午":
//                case "晚上":
//                case "午休": case "晚饭": case "":
//                    break;
            }
        }
    }
}
