package scripts.task_custom_field_value;

import com.trackstudio.app.Slider;
import com.trackstudio.app.adapter.AdapterManager;
import com.trackstudio.app.filter.FValue;
import com.trackstudio.app.filter.TaskFValue;
import com.trackstudio.app.filter.list.TaskFilter;
import com.trackstudio.exception.GranException;
import com.trackstudio.external.TaskUDFValueScript;
import com.trackstudio.kernel.manager.KernelManager;
import com.trackstudio.secured.SecuredResolutionBean;
import com.trackstudio.secured.SecuredTaskBean;
import com.trackstudio.secured.SecuredUDFValueBean;
import com.trackstudio.tools.tree.OrderedTree;

import java.text.DateFormatSymbols;
import java.util.*;


public class ReportTicketDistribution implements TaskUDFValueScript {

    public String filterId, title;
    public static final String DEPARTMENT = "ff80818131f536730131f5e89be9011a";
    public static final String MONTH = "ff80818131f536730131f58302410077";

    public ReportTicketDistribution() {
        this.filterId = "ff80818131f536730131f59289bc00e1";
        this.title = " оличество за€вок исполненных/отозванных/за€вленных за период времени";
    }

    protected class Row implements Comparable{
        private String month, state, department, id;

        protected Row(String month, String state, String department, String id) {
            this.month = month;
            this.state = state;
            this.department = department;
            this.id = id;
        }

        public String getMonth() {
            return month;
        }

        public String getState() {
            return state;
        }

        public String getDepartment() {
            return department;
        }

        public String getId() {
            return id;
        }

        public String toString(){
            return month+":"+state+":"+department+":"+id;
        }
        public int compareTo(Object o) {
                        return this.toString().compareTo(o.toString());
        }
    }

    public Object calculate(SecuredTaskBean securedTaskBean) throws GranException {
        TaskFilter taskList = new TaskFilter(securedTaskBean);
        TaskFValue taskFValue = KernelManager.getFilter().getTaskFValue(filterId);
        Row root = new Row(":", "", "", "");
        OrderedTree<Row> data = new OrderedTree<Row>(root);
        /*
        root-month-resolution-department
         */
        ArrayList<SecuredTaskBean> taskCol = taskList.getTaskList(taskFValue, true, false, null);
        ArrayList<String> monthes = new ArrayList<String>();
        DateFormatSymbols symbols = new DateFormatSymbols(securedTaskBean.getSecure().getUser().getDateFormatter().getLocale());
        
        for (String s: symbols.getMonths()){
          if (s.length()>0)  monthes.add(s);

        }

        
        ArrayList<String> states = new ArrayList<String>();
        HashMap<String, Integer> departments = new HashMap<String, Integer>();
        HashMap<String, String> ul = null;
       for (SecuredTaskBean task : taskCol) {
             HashMap<String, SecuredUDFValueBean> m = task.getUDFValues();
             SecuredUDFValueBean departmentUdf = m.get(DEPARTMENT);
             SecuredUDFValueBean monthUdf = m.get(MONTH);
           if (ul==null) ul = monthUdf.getUdf().getUL();
           String department = null, month = null;
           if (departmentUdf!=null){
               Object v = departmentUdf.getValue();
               if (v!=null) department = v.toString();
           }
           if (monthUdf!=null && ul!=null){
               Object v = monthUdf.getValue();
               if (v!=null) month = ul.get(v.toString());
           }
           
           if (!departments.containsKey(department)) departments.put(department,0);


           SecuredResolutionBean securedResolutionBean = task.getResolution();
           String state = "null";
           if (securedResolutionBean!=null)
           state = securedResolutionBean.getName();
           if (!states.contains(state)) states.add(state);
           Row rowMonth = new Row(month, "", "", "");
           Row rowState = new Row(month, state, "", "");
           Row rowDepartment = new Row(month, state, department, "");
           Row row = new Row(month, state, department, task.getId());

           if (!data.contains(rowMonth)){
               data.add(root, rowMonth);
           }
           if (!data.contains(rowState)) data.add(rowMonth, rowState);
           if (!data.contains(rowDepartment)) data.add(rowState, rowDepartment);
           data.add(rowDepartment, row);
       }

       StringBuffer result = new StringBuffer();
       result.append("<style class=\"text/css\">");
                result.append("/*plugin styles*/\n" +
                        ".visualize { border: 1px solid #888; position: relative; background: #fafafa; }\n" +
                        ".visualize canvas { position: absolute; }\n" +
                        ".visualize ul,.visualize li { margin: 0; padding: 0;}\n" +
                        "\n" +
                        "/*table title, key elements*/\n" +
                        ".visualize .visualize-info { padding: 3px 5px; background: #fafafa; border: 1px solid #888; position: absolute; top: -20px; right: 10px; opacity: .8; }\n" +
                        ".visualize .visualize-title { display: block; color: #333; margin-bottom: 3px;  font-size: 1.1em; }\n" +
                        ".visualize ul.visualize-key { list-style: none;  }\n" +
                        ".visualize ul.visualize-key li { list-style: none; float: left; margin-right: 10px; padding-left: 10px; position: relative;}\n" +
                        ".visualize ul.visualize-key .visualize-key-color { width: 6px; height: 6px; left: 0; position: absolute; top: 50%; margin-top: -3px;  }\n" +
                        ".visualize ul.visualize-key .visualize-key-label { color: #000; }\n" +
                        "\n" +
                        "/*line,bar, area labels*/\n" +
                        ".visualize-labels-x,.visualize-labels-y { position: absolute; left: 0; top: 0; list-style: none; }\n" +
                        ".visualize-labels-x li, .visualize-labels-y li { position: absolute; bottom: 0; }\n" +
                        ".visualize-labels-x li span.label, .visualize-labels-y li span.label { position: absolute; color: #555;  }\n" +
                        ".visualize-labels-x li span.line, .visualize-labels-y li span.line {  position: absolute; border: 0 solid #ccc; }\n" +
                        ".visualize-labels-x li { height: 100%; }\n" +
                        ".visualize-labels-x li span.label { top: 100%; margin-top: 50px; }\n" +
                        ".visualize-labels-x li span.line { border-left-width: 1px; height: 100%; display: block; }\n" +
                        ".visualize-labels-x li span.line { border: 0;} /*hide vertical lines on area, line, bar*/\n" +
                        ".visualize-labels-y li { width: 100%;  }\n" +
                        ".visualize-labels-y li span.label { right: 100%; margin-right: 5px; display: block; width: 100px; text-align: right; }\n" +
                        ".visualize-labels-y li span.line { border-top-width: 1px; width: 100%; }\n" +
                        ".visualize-bar .visualize-labels-x li span.label { width: 100%; text-align: center; }\n"+

                        ".visualize { margin: 60px 0 0 30px; padding: 70px 40px 160px; background: #ccc url(../images/chartbg-vanilla.png) top repeat-x; border: 1px solid #ddd; -moz-border-radius: 12px; -webkit-border-radius: 12px; border-radius: 12px; }\n" +
                        ".visualize canvas { border: 1px solid #aaa; margin: -1px; background: #fff; }\n" +
                        ".visualize-labels-x { top: 90px; left: 40px; z-index: 100; }\n" +
                        ".visualize-labels-y { top: 70px; left: 40px; z-index: 100; }\n" +
                        ".visualize-pie .visualize-labels { position: absolute; top: 70px; left: 40px; }\n" +
                        ".visualize-labels-y li span.label { color: #444; font-size: 10px; padding-right: 5px;}\n"+
                        ".visualize-labels-x li span.label { display: block; color: #444; font-size: 10px; padding-right: 0px;}\n"+
                        ".visualize-labels-x li span.label em {display: block; font-style: normal; color: #444; font-size: 9px; padding-top: 0px;" +

                        " transform: rotate(-90deg);  -moz-transform: rotate(-90deg);\n" +
                        "-webkit-transform: rotate(-90deg);\n" +
                        "-o-transform: rotate(-90deg);\n" +

                        "writing-mode: tb-rl;}\n" +
                        ".visualize-labels-y li span.line { border-style: solid;  opacity: .7; }\n" +
                        ".visualize .visualize-info { border: 0; position: static;  opacity: 1; background: none; }\n" +
                        ".visualize .visualize-title { position: absolute; top: 20px; color: #333; margin-bottom: 0; left: 20px; font-size: 12px; font-weight: bold; }\n" +
                        ".visualize ul.visualize-key { position: absolute; bottom: 15px; background: #eee; z-index: 10; padding: 10px 0; color: #aaa; width: 100%; left: 0;  }\n" +
                        ".visualize ul.visualize-key li { font-size: 1.2em;  margin-left: 20px; padding-left: 18px; }\n" +
                        ".visualize ul.visualize-key .visualize-key-color { width: 10px; height: 10px;  margin-top: -4px; }\n" +
                        ".visualize ul.visualize-key .visualize-key-label { color: #333; }"
                );
                result.append("</style>");
                result.append("<script type=\"text/javascript\" src=\"/TrackStudio/html/visualize/jquery.min.js\"></script>");
                result.append("<script type=\"text/javascript\" src=\"/TrackStudio/html/visualize/visualize.jQuery.js\"></script>");
        result.append("<table id=\"").append(filterId).append("\" style=\"display: none\">");
                result.append("<caption>").append(title).append("</caption>");
                    result.append("<tr>");
                    result.append("<td></td>");
                for (String dep: departments.keySet()){
                    result.append("<th  scope=\"col\">").append(dep).append("</th>");
                }
                    result.append("</tr>");
                    

                for (String m: monthes){
                    boolean first = true;
                    for (String s: states){
                        result.append("<tr><th scope=\"row\">");
                        result.append("<em>").append(m).append("");
                        result.append("<br/><b>").append(s).append("</b></em>");
                        result.append("</th>");
                        Set<String> deps = Collections.unmodifiableSet(departments.keySet());
                        for (String d: deps){
                            Row dep = new Row(m, s, d, "");
                            int tasks = data.getChildrenCount(dep);
                            if (tasks<0) tasks = 0;
                            else {
                            Integer now = departments.get(d);
                            departments.put(d, now+tasks);
                            }
                            result.append("<td>");
                            result.append(tasks);
                            result.append("</td>");
                        }
                        result.append("</tr>");
                    }
                }
                result.append("<tr><th scope=\"row\">");
                result.append("<em><b>¬сего</b></em>");
                result.append("</th>");

                for (String d: departments.keySet()){
                    result.append("<td>");
                    result.append(departments.get(d));
                    result.append("</td>");
                }
                result.append("</tr>");

                result.append("</table>");
                result.append("<script>");
        result.append("$('table#").append(filterId).append("').visualize({type: 'bar', parseDirection: 'y', height: '200px', width: '800px', barGroupMargin: '4'});");
                result.append("</script>");

       return result.toString();
    }
}
