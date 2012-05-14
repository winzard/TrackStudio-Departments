package scripts.task_custom_field_value;

import com.trackstudio.app.filter.TaskFValue;
import com.trackstudio.app.filter.list.TaskFilter;
import com.trackstudio.exception.GranException;
import com.trackstudio.kernel.manager.KernelManager;
import com.trackstudio.secured.SecuredTaskBean;
import com.trackstudio.secured.SecuredUDFValueBean;
import com.trackstudio.tools.tree.OrderedTree;

import java.text.DateFormatSymbols;
import java.util.*;


public class ReportOverdueTickets extends ReportTicketDistribution{
    private static final String SOME_DATE = "ff80818131e5e6600131e621bd3b00bd";

    public ReportOverdueTickets() {
        this.filterId = "ff80818131f536730131f601bb5b01cc";
        this.title= "Количество заявок, исполненных и исполненных с опозданием";
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
         states.add( "исполнено");
         states.add("с&nbsp;опозданием");
       for (SecuredTaskBean task : taskCol) {
             HashMap<String, SecuredUDFValueBean> m = task.getUDFValues();
           SecuredUDFValueBean udf = m.get(SOME_DATE);
        String state = "";
        Calendar date = null;
        if (udf != null) {
            Object v = udf.getValue();
            if (v != null) {
                date = (Calendar) v;
            }
        }
        if (date == null) {
            if (task.getClosedate() != null) date = task.getClosedate();
            else date = null;
        }
        if (task.getDeadline()!=null && task.getDeadline().before(date)) state="с&nbsp;опозданием";
        else state = "исполнено";

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
        result.append("<table id=\"").append(filterId).append("\" style=\"display: none\">");
                result.append("<caption>").append(title).append("</caption>");
                    result.append("<tr>");
                    result.append("<td></td>");
                for (String dep: departments.keySet()){
                    result.append("<th  scope=\"col\">").append(dep).append("</th>");
                }
                    result.append("</tr>");


                for (String m: monthes){
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
                result.append("<em><b>Всего</b></em>");
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
