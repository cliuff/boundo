package com.madness.collision.unit.school_timetable.parser;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.util.ArrayUtils;
import com.madness.collision.R;
import com.madness.collision.unit.school_timetable.MyUnit;
import com.madness.collision.unit.school_timetable.data.CourseSingleton;
import com.madness.collision.unit.school_timetable.data.Repetition;
import com.madness.collision.unit.school_timetable.data.Timetable;
import com.madness.collision.util.F;
import com.madness.collision.util.X;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;

/**
 * Parse from standard timetable
 */
class StandardParser {

    private int phraseCount = 0;
    private boolean isAfternoon = false;
    private boolean isEvening = false;
    private char template = 'a';
    private int morningClassCount = 0;
    private int afternoonClassCount = 0;
    private CourseSingleton cs = new CourseSingleton();
    private HashMap<String, String> descriptionMap = new HashMap<>();
    private String bridge_path;
    Timetable timetable = new Timetable();

    private Context context;

    private int startWeek;
    private int endWeek;

    StandardParser(Context context){
        this.context = context;
        bridge_path = F.INSTANCE.valCachePubTtBridge(context);
    }

    void parse(ParseResult content){
        template = content.getTemplate();
        preToBe(content.getRe());
    }

    private void save2File(String processText){
        try {
            File bridgeFile = new File(bridge_path);
            if (!F.INSTANCE.prepare4(bridgeFile)) return;
            FileOutputStream fileOutputStream = new FileOutputStream(bridgeFile);
            fileOutputStream.write(processText.getBytes());
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void preToBe(String processText){
        if (template == 'c') {
            String description = processText.substring(processText.lastIndexOf("?#wrapHEAD"));
            processText = processText.replace(description, "");
            descriptionMap = TimetableParser.resolveDescription(description);
        }
        save2File(processText);

        ParseResult rawResult = TimetableParser.confirmStandard(context, processText);
        boolean implementInput = rawResult.getTemplate() == '1';
        if (implementInput){
            TimetableParser.getInput(context, rawResult.getRe(), (startWeek1, endWeek1) -> {
                startWeek = startWeek1;
                endWeek = endWeek1;
                String result = processBridgeFile();
                if (result == null)
                    X.INSTANCE.toast(context, com.madness.collision.unit.school_timetable.R.string.ics_Toast_classtime_generating_fail, Toast.LENGTH_SHORT);
                else if (result.equals("done"))
                    X.INSTANCE.toast(context, com.madness.collision.unit.school_timetable.R.string.ics_Toast_generating_success, Toast.LENGTH_SHORT);
                timetable.persist(context, true);
                MyUnit.Companion.setTable(context, timetable);
            });
        }else {
            String result = processBridgeFile();
            if (result == null)
                X.INSTANCE.toast(context, com.madness.collision.unit.school_timetable.R.string.ics_Toast_classtime_generating_fail, Toast.LENGTH_SHORT);
            else if (result.equals("done"))
                X.INSTANCE.toast(context, com.madness.collision.unit.school_timetable.R.string.ics_Toast_generating_success, Toast.LENGTH_SHORT);
            else if (result.equals("!"))
                X.INSTANCE.toast(context, com.madness.collision.unit.school_timetable.R.string.ics_toast_errortobedelt, Toast.LENGTH_SHORT);
            timetable.persist(context, true);
            MyUnit.Companion.setTable(context, timetable);
        }
    }

    private String processBridgeFile(){
        try {
            return process();
        }catch (Exception e){
            e.printStackTrace();
            X.INSTANCE.toast(context, R.string.text_error, Toast.LENGTH_SHORT);
        }
        return "";
    }

    private Scanner loadFromFile(){
        File file = new File(bridge_path);
        if (!file.exists()) return null;
        try {
            return new Scanner(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String process(){
        // 生成课表文件
        Scanner scanner_get_text = loadFromFile();
        int weekdayCount = 1;
        int terminal_class = 0;
        boolean endReached = false;
        String textString;
        if (scanner_get_text == null) return null;
        while (scanner_get_text.hasNextLine()) {
            if (endReached) {
                textString = "?#empty";
                endReached = false;
            } else
                textString = scanner_get_text.nextLine();
            //处理几节/周
            if (textString.contains("节/周") && !textString.contains("{")) {
                cs.getEducating().setRepetitions(new Repetition[]{new Repetition(startWeek, endWeek, Repetition.WEEKS_ALL)});
                continue;
            }
            if (textString.contains("?#initiate")) {
                terminal_class = Integer.parseInt(textString.replaceAll("[\\D]", ""));
                continue;
            }
            if (textString.contains("(换") || textString.contains("(调") || textString.contains("(停") ||
                    textString.contains("考试时间") || textString.contains("考试地点") ||
                    (textString.contains(":") && textString.contains("-") && textString.startsWith("(") && textString.endsWith(")】"))) {
                phraseCount = 0;
                continue;
            }
            if (textString.startsWith("课号：")){
                phraseCount = 0;
                timetable.getCourses().add(cs);
                char previousClassPeriod = cs.getLegacyClassPeriod();
                int previousCourseWeekday = cs.getEducating().getDayOfWeek();
                cs = new CourseSingleton();
                cs.setLegacyClassPeriod(previousClassPeriod);
                cs.getEducating().setDayOfWeek(previousCourseWeekday);
                continue;
            }
            if (isAfternoon){
                morningClassCount = terminal_class - 1;
                isAfternoon = false;
            }
            if (isEvening){
                afternoonClassCount = terminal_class - morningClassCount - 1;
                isEvening = false;
            }


            switch (textString) {
                case "?#wrapHEAD":
                    phraseCount = 1;
                    weekdayCount = 1;
                    continue;
                case "?#period1":
                    cs.setLegacyClassPeriod('a');
                    continue;
                case "?#period2":
                    cs.setLegacyClassPeriod('b');
                    continue;
                case "?#period3":
                    cs.setLegacyClassPeriod('c');
                    continue;
                case "?#filled":
                    phraseCount ++;
                    continue;
                case "?#empty":
                    timetable.getCourses().add(cs);
                    char previousClassPeriod = cs.getLegacyClassPeriod();
                    int previousCourseWeekday = cs.getEducating().getDayOfWeek();
                    cs = new CourseSingleton();
                    cs.setLegacyClassPeriod(previousClassPeriod);
                    cs.getEducating().setDayOfWeek(previousCourseWeekday);
                    phraseCount = 0;
                    continue;
                case "?#wrapTOE": case "?#null": case "?#morning":
                    continue;
                case "?#afternoon":
                    isAfternoon = true;
                    continue;
                case "?#evening":
                    isEvening = true;
                    continue;
                case "?#terminal":
                    if (template == 'd' && phraseCount == 1) {
                        cs = new CourseSingleton();
                        continue;
                    }
                    timetable.getCourses().add(cs);
                    phraseCount = 1;
                    weekdayCount++;
                    cs = new CourseSingleton();
                    continue;
                case "必修":case "选修":case "学类":
                    case "通识":case "基础":case "学门":
                        case "人文": case "[教学大纲|授课计划]": case "任选":
                case "学选": case "专选":
                case "":
                    phraseCount --;
                    continue;
                default:
                    break;
            }

            if (phraseCount == 1) {
                cs.setName(textString);
                cs.getEducating().setDayOfWeek(weekdayCount);

                if (template == 'd'){
                    classPhaseD(terminal_class);
                }else {
                    String phase = classPhase(terminal_class);
                    if (!phase.isEmpty()) cs.setLegacyClassPhase(phase);
                }

                if (template == 'c'){
                    textString = descriptionMap.get(textString);
                    if (textString != null)
                        cs.getEducating().setEducator(textString);
                }
            } else if (phraseCount == 2) {
                if (textString.endsWith(")") && textString.indexOf("(") > textString.indexOf("-"))
                    template = 'b';
                cs.setLegacyTemplate(template);
                //course period
                String coursePeriod = "";
                switch (template){
                    case 'a':
                        coursePeriod = getPeriodA(textString);
                        break;
                    case 'b':
                        coursePeriod = textString.substring(0, textString.indexOf("("));
                        break;
                    case 'c':
                        coursePeriod = textString.substring(textString.indexOf("(") + 1, textString.indexOf(")"));
                        if (textString.endsWith("*")){
                            cs.getEducating().setLocation(textString.substring(textString.indexOf(")") + 1, textString.indexOf("*")));
                        }
                        break;
                    case 'd':
                        cs.getEducating().setLocation(textString);
                        break;
                }
                cs.getEducating().setRepetitions(Repetition.Companion.parseRaw(coursePeriod));
            } else if (phraseCount == 3) {
                if (template != 'c')
                    cs.getEducating().setEducator(textString);
            } else if (phraseCount == 4) {
                if (template == 'd'){
                    cs.getEducating().setRepetitions(Repetition.Companion.parseRaw(textString));
                }else {
                    cs.getEducating().setLocation(textString);
                }
                endReached = true;
            }
        }
        scanner_get_text.close();
        File file = new File(bridge_path);
        if (!file.delete()) Log.d("progress", "Deleting extra file fails");

        removeEmptyCourses();

        if (template == 'd') timetable.renderUID();

        mergeWeek();
        timetable.renderUID();

        boolean re = timetable.produceICal(context);
        timetable.renderTimetable();
        return re ? "done" : "!";
    }

    private void classPhaseD(int terminal_class){
        //class phase
        if (morningClassCount == 0 && afternoonClassCount == 0){
            switch (terminal_class) {
                case 1: case 2:
                    cs.setLegacyClassPhase("11");
                    cs.setLegacyClassPeriod('a');
                    break;
                case 3: case 4: case 5:
                    cs.setLegacyClassPhase("12");
                    cs.setLegacyClassPeriod('a');
                    break;
                default:
                    break;
            }
        }else if (terminal_class > morningClassCount && afternoonClassCount == 0){
            switch (terminal_class - morningClassCount) {
                case 1: case 2:
                    cs.setLegacyClassPhase("21");
                    cs.setLegacyClassPeriod('a');
                    break;
                case 3: case 4: case 5:
                    cs.setLegacyClassPhase("22");
                    cs.setLegacyClassPeriod('a');
                    break;
                default:
                    break;
            }
        }else{
            switch (terminal_class - (morningClassCount + afternoonClassCount)) {
                case 1: case 2: case 3:
                    cs.setLegacyClassPhase("31");
                    cs.setLegacyClassPeriod('a');
                    break;
                default:
                    break;
            }
        }
    }

    private String classPhase(int terminal_class){
        //class phase
        if (morningClassCount == 0 && afternoonClassCount == 0){
            switch (terminal_class) {
                case 1: return "11";
                case 3: return "12";
                case 5: return "13";
            }
        }else if (terminal_class > morningClassCount && afternoonClassCount == 0){
            switch (terminal_class - morningClassCount) {
                case 1: return "21";
                case 3: return "22";
                case 5: return "23";
            }
        }else{
            switch (terminal_class - (morningClassCount + afternoonClassCount)) {
                case 1: return "31";
                case 3: return "32";
                case 5: return "33";
            }
        }
        return "";
    }

    private String getPeriodA(String textString){
        String coursePeriod = textString.substring(textString.indexOf("{") + 2, textString.indexOf("}") - 1);
        //{第3-17周|3节/周}， {第1-15周|单周}
        if (coursePeriod.contains("周|单") || coursePeriod.contains("周|双")) {
            coursePeriod = coursePeriod.replace("周|", "");
        }
        if (coursePeriod.contains("节/")) {
            coursePeriod = coursePeriod.substring(0, coursePeriod.indexOf("周"));
        }
        return coursePeriod;
    }

    //remove null ones
    private void removeEmptyCourses(){
        Iterator<CourseSingleton> courseIterator = timetable.getCourses().iterator();
        while (courseIterator.hasNext()){
            String cn = courseIterator.next().getName();
            if (cn.isEmpty() || cn.trim().isEmpty())
                courseIterator.remove();
        }
    }

    private void mergeWeek(){
        Iterator<CourseSingleton> courseIterator = timetable.getCourses().iterator();
        while (courseIterator.hasNext()){
            cs = courseIterator.next();
            if (cs == null)
                continue;
            for (CourseSingleton iterateCourse : timetable.getCourses()){
                if (iterateCourse == null)
                    continue;
                if (cs.getEducating().getDayOfWeek() != iterateCourse.getEducating().getDayOfWeek() ||
                        !cs.getLegacyClassPhase().equals(iterateCourse.getLegacyClassPhase()))
                    continue;
                if (template == 'd'){
                    if (cs.getLegacyUid().equals(iterateCourse.getLegacyUid())
                            && !cs.equals(iterateCourse)
                            && cs.getLegacyClassPeriod() <= iterateCourse.getLegacyClassPeriod()) {
                        courseIterator.remove();
                        iterateCourse.setLegacyClassPeriod((char) (iterateCourse.getLegacyClassPeriod() + 1));
                        break;
                    }
                }
                //equals() is for string whereas matches() is for pattern, "形势与政策（6）".matches("形势与政策（6）") returned false
                cs.getName();
                if (!cs.getName().equals(iterateCourse.getName()))
                    continue;
                else {
                    if (!cs.getEducating().getEducator().equals(iterateCourse.getEducating().getEducator()))
                        continue;
                    else if (!cs.getEducating().getLocation().equals(iterateCourse.getEducating().getLocation()))
                        continue;
                    else if (Arrays.equals(cs.getEducating().getRepetitions(), iterateCourse.getEducating().getRepetitions()))
                        continue;
                }
                iterateCourse.getEducating().setRepetitions(sortRepetitions(iterateCourse, cs));
                courseIterator.remove();
                break;
            }
        }
    }

    private Repetition[] sortRepetitions(CourseSingleton course1, CourseSingleton course2){
        Repetition[] repetitions = course1.getEducating().getRepetitions();
        Repetition[] repetitions2Add = course2.getEducating().getRepetitions();
        Repetition[] newRepetitions = ArrayUtils.concat(repetitions, repetitions2Add);
        Comparator<Repetition> comparator;
        if (X.INSTANCE.aboveOn(X.N)){
            comparator = Comparator.comparingInt(Repetition::getFromWeek);
        }else {
            comparator = (repetition, t1) -> {
                if (t1.getFromWeek() == repetition.getFromWeek()) return 0;
                return t1.getFromWeek() > repetition.getFromWeek() ? -1 : 1;
            };
        }
        Arrays.sort(newRepetitions, comparator);
        return newRepetitions;
    }
}
