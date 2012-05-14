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


public class ReportSatisfaction extends ReportTicketDistribution{
    private static final String SATISFACTION_UDF = "ff80818131cd4e930131cd62d39a0042";
    private static final String GOOD = "ff80818131cd4e930131cd62d3b60045";
     protected class Satisfaction implements Comparable{
        private String month, department;

        public Satisfaction(String month, String department) {
           this.month = month;
           this.department = department;
        }

        protected Integer satisfied=0, unsatisfied=0;

         public Integer getSatisfied() {
             return satisfied;
         }

         public void setSatisfied(Integer satisfied) {
             this.satisfied = satisfied;
         }

         public Integer getUnsatisfied() {
             return unsatisfied;
         }

         public void setUnsatisfied(Integer unsatisfied) {
             this.unsatisfied = unsatisfied;
         }

         public void addSatisfied(boolean sat){
            if (sat) this.satisfied++;
             else this.unsatisfied++;
        }
        

        public String toString(){
            return month+":"+department;
        }
        public int compareTo(Object o) {
                        return this.toString().compareTo(o.toString());
        }
    }
    public ReportSatisfaction() {
        this.filterId = "ff80818131f536730131f6170e33023c";
        this.title= "Удовлетворенность исполнением заявок";
    }

    

    public Object calculate(SecuredTaskBean securedTaskBean) throws GranException {
        TaskFilter taskList = new TaskFilter(securedTaskBean);
        TaskFValue taskFValue = KernelManager.getFilter().getTaskFValue(filterId);
        Satisfaction root = new Satisfaction("ff80818131f536730131f608bd6e01f0", "ff80818131f536730131f608bd6e01f0");
        OrderedTree<Satisfaction> data = new OrderedTree<Satisfaction>(root);
        /*
        root-month-resolution-department
         */

        ArrayList<SecuredTaskBean> taskCol = taskList.getTaskList(taskFValue, true, false, null);
        ArrayList<String> monthes = new ArrayList<String>();
        DateFormatSymbols symbols = new DateFormatSymbols(securedTaskBean.getSecure().getUser().getDateFormatter().getLocale());
        for (String s: symbols.getMonths()){
          if (s.length()>0)  monthes.add(s);
        }


        ArrayList<String> departments = new ArrayList<String>();
        HashMap<String, String> ul = null;

       for (SecuredTaskBean task : taskCol) {
             HashMap<String, SecuredUDFValueBean> m = task.getUDFValues();
           SecuredUDFValueBean udf = m.get(SATISFACTION_UDF);
           boolean satisfied = false;
            if (udf!=null){
        Object v = udf.getValue();
        satisfied = (v!=null && v.toString().equals(GOOD));
            }
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

           if (!departments.contains(department)) departments.add(department);
           Satisfaction rowMonth = new Satisfaction(month, "-");
           Satisfaction rowTotal = new Satisfaction("Итого", "-");
           Satisfaction rowDepartment = new Satisfaction(month, department);
           Satisfaction totalDepartment = new Satisfaction("Итого", department);

           if (!data.contains(rowMonth)){
               data.add(root, rowMonth);
           }


           if (!data.contains(rowDepartment)) data.add(rowMonth, rowDepartment);
           if (!data.contains(rowTotal)) data.add(root, rowTotal);
           if (!data.contains(totalDepartment)) data.add(rowTotal, totalDepartment);

           Satisfaction rc = data.get(rowDepartment);
           rc.addSatisfied(satisfied);

           Satisfaction parent = data.getParent(rc);
           parent.addSatisfied(satisfied);

           Satisfaction rt = data.getRoot();
           rt.addSatisfied(satisfied);


           Satisfaction totDep = data.get(totalDepartment);
           totDep.addSatisfied(satisfied);
            
       }

       StringBuffer result = new StringBuffer();
        result.append("<table id=\"").append(filterId).append("\" style=\"display: none\">");
                result.append("<caption>").append(title).append("</caption>");
                    result.append("<tr>");
                    result.append("<td></td>");
                for (String dep: departments){
                    result.append("<th  scope=\"col\">").append(dep).append("</th>");
                }
         result.append("<th  scope=\"col\">").append("Итого").append("</th>");
                    result.append("</tr>");



                for (String m: monthes){

                        result.append("<tr><th scope=\"row\">");
                        result.append("<em>").append(m).append("");
                        result.append("<br/><b>").append("исполненные").append("</b></em>");
                        result.append("</th>");
                        for (String d: departments){
                            Satisfaction dep = new Satisfaction(m, d);
                            result.append("<td>");
                            if (data.contains(dep)){
                                ReportSatisfaction.Satisfaction satisfaction = data.get(dep);
                                Integer dc = satisfaction.getSatisfied()+ satisfaction.getUnsatisfied();
                            result.append(dc);
                            } else result.append("0");
                            result.append("</td>");
                        }

                    Satisfaction rm = new Satisfaction(m, "-");
                    Satisfaction rowMonth = data.get(rm);
                        result.append("<td>");
                    if (rowMonth!=null)
                        result.append(rowMonth.getSatisfied()+rowMonth.getUnsatisfied());
                    else result.append("0");
                        result.append("</td>");
                        result.append("</tr>");

                        result.append("<tr><th scope=\"row\">");
                        result.append("<em>").append(m).append("");
                        result.append("<br/><b>").append("с&nbsp;замечаниями").append("</b></em>");
                        result.append("</th>");


                        for (String d: departments){
                            Satisfaction dep = new Satisfaction(m, d);
                            result.append("<td>");
                            if (data.contains(dep)){
                            Integer dc = data.get(dep).getUnsatisfied();
                            result.append(dc);
                            } else result.append("0");
                            result.append("</td>");
                        }
                        result.append("<td>");
                    if (rowMonth!=null)
                            result.append(rowMonth.getUnsatisfied());
                    else result.append("0");
                            result.append("</td>");
                        result.append("</tr>");

                }
                result.append("<tr><th scope=\"row\">");
                result.append("<em><b>Всего исполненно</b></em>");
                result.append("</th>");

                for (String d: departments){
                    Satisfaction totalDepartment = new Satisfaction("Итого", d);
                    result.append("<td>");
                    ReportSatisfaction.Satisfaction satisfaction = data.get(totalDepartment);
                    result.append(satisfaction.getSatisfied()+satisfaction.getUnsatisfied());
                    result.append("</td>");
                }
                    result.append("<td>");
                    result.append(data.getRoot().getSatisfied()+data.getRoot().getUnsatisfied());
                    result.append("</td>");
                result.append("</tr>");

                result.append("<tr><th scope=\"row\">");
                result.append("<em><b>Всего с замечаниями</b></em>");
                result.append("</th>");

                for (String d: departments){
                    Satisfaction totalDepartment = new Satisfaction("Итого", d);
                    result.append("<td>");
                    result.append(data.get(totalDepartment).getUnsatisfied());
                    result.append("</td>");
                }
                    result.append("<td>");
                    result.append(data.getRoot().getUnsatisfied());
                    result.append("</td>");
                result.append("</tr>");
                result.append("</table>");
                result.append("<script>");
        result.append("$('table#").append(filterId).append("').visualize({type: 'bar', parseDirection: 'y', height: '200px', width: '800px', barGroupMargin: '4'});");
                result.append("</script>");

       return result.toString();
    }
}
