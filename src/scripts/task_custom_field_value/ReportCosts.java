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


public class ReportCosts extends ReportTicketDistribution{
    private static final String PLANNED = "ff80818131e11fca0131e1364aa10054";
    private static final String REAL = "ff80818131e11fca0131e13969c400a9";
    
    public ReportCosts() {
        this.filterId = "ff80818131f536730131f608bd6e01f0";
        this.title= "Затраты на исполнение заявок";
    }


    protected class Costs implements Comparable{
        private String month, department;

        public Costs(String month, String department) {
           this.month = month;
           this.department = department;
        }

        protected Double real =0d;
        protected Double planned =0d;

        public Double getPlanned() {
            return planned;
        }

        public void setPlanned(Double planned) {
            this.planned = planned;
        }

        public Double getReal() {
            return real;
        }
        public void addReal(Double cost){
            this.real = this.real +cost;
        }
        public void addPlanned(Double cost){
            this.planned = this.planned +cost;
        }
        public void setReal(Double real) {
            this.real = real;
        }

        public String toString(){
            return month+":"+department;
        }
        public int compareTo(Object o) {
                        return this.toString().compareTo(o.toString());
        }
    }

     public Object calculate(SecuredTaskBean securedTaskBean) throws GranException {
        TaskFilter taskList = new TaskFilter(securedTaskBean);
        TaskFValue taskFValue = KernelManager.getFilter().getTaskFValue(filterId);
        Costs root = new Costs("ff80818131f536730131f608bd6e01f0", "ff80818131f536730131f608bd6e01f0");
        OrderedTree<Costs> data = new OrderedTree<Costs>(root);
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
           SecuredUDFValueBean plannedUdf = m.get(PLANNED);
           SecuredUDFValueBean Udf = m.get(REAL);
        Double plannedCosts=0d, realCosts=0d;
        
        if (plannedUdf != null) {
            Object v = plannedUdf.getValue();
            if (v != null) {
                try{
                plannedCosts = (Double) v;
                } catch (NumberFormatException e){};
            }
        }
           if (Udf != null) {
            Object v = Udf.getValue();
            if (v != null) {
                try{
                realCosts = (Double) v;
                } catch (NumberFormatException e){};
            }
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
           Costs rowMonth = new Costs(month, "-");
           Costs rowTotal = new Costs("Итого", "-");
           Costs rowDepartment = new Costs(month, department);
           Costs totalDepartment = new Costs("Итого", department);

           if (!data.contains(rowMonth)){
               data.add(root, rowMonth);
           }


           if (!data.contains(rowDepartment)) data.add(rowMonth, rowDepartment);
           if (!data.contains(rowTotal)) data.add(root, rowTotal);
           if (!data.contains(totalDepartment)) data.add(rowTotal, totalDepartment);
           
           Costs rc = data.get(rowDepartment);
           rc.addReal(realCosts);
           rc.addPlanned(plannedCosts);
           Costs parent = data.getParent(rc);
           parent.addReal(realCosts);
           parent.addPlanned(plannedCosts);
           Costs rt = data.getRoot();
           rt.addReal(realCosts);
           rt.addPlanned(plannedCosts);
           //Costs tot = data.get(rowTotal);
           Costs totDep = data.get(totalDepartment);
           totDep.addReal(realCosts);
           totDep.addPlanned(plannedCosts);
       //    data.add(rowTotal, totDep);

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
                        result.append("<br/><b>").append("планируемые").append("</b></em>");
                        result.append("</th>");
                        for (String d: departments){
                            Costs dep = new Costs(m, d);
                            result.append("<td>");
                            if (data.contains(dep)){
                            Double dc = data.get(dep).getPlanned();
                            result.append(dc);
                            } else result.append("0"); 
                            result.append("</td>");
                        }

                    Costs rm = new Costs(m, "-");
                    Costs rowMonth = data.get(rm);
                        result.append("<td>");
                    if (rowMonth!=null)
                        result.append(rowMonth.getPlanned());
                    else result.append("0"); 
                        result.append("</td>");
                        result.append("</tr>");

                        result.append("<tr><th scope=\"row\">");
                        result.append("<em>").append(m).append("");
                        result.append("<br/><b>").append("фактические").append("</b></em>");
                        result.append("</th>");


                        for (String d: departments){
                            Costs dep = new Costs(m, d);
                            result.append("<td>");
                            if (data.contains(dep)){
                            Double dc = data.get(dep).getReal();
                            result.append(dc);
                            } else result.append("0");
                            result.append("</td>");
                        }
                        result.append("<td>");
                    if (rowMonth!=null)
                            result.append(rowMonth.getReal());
                    else result.append("0");
                            result.append("</td>");
                        result.append("</tr>");

                }
                result.append("<tr><th scope=\"row\">");
                result.append("<em><b>Всего планируемых</b></em>");
                result.append("</th>");

                for (String d: departments){
                    Costs totalDepartment = new Costs("Итого", d);
                    result.append("<td>");
                    result.append(data.get(totalDepartment).getPlanned());
                    result.append("</td>");
                }
                    result.append("<td>");
                    result.append(data.getRoot().getPlanned());
                    result.append("</td>");
                result.append("</tr>");

                result.append("<tr><th scope=\"row\">");
                result.append("<em><b>Всего фактических</b></em>");
                result.append("</th>");

                for (String d: departments){
                    Costs totalDepartment = new Costs("Итого", d);
                    result.append("<td>");
                    result.append(data.get(totalDepartment).getReal());
                    result.append("</td>");
                }
                    result.append("<td>");
                    result.append(data.getRoot().getReal());
                    result.append("</td>");
                result.append("</tr>");
                result.append("</table>");
                result.append("<script>");
        result.append("$('table#").append(filterId).append("').visualize({type: 'bar', parseDirection: 'y', height: '200px', width: '800px', barGroupMargin: '4'});");
                result.append("</script>");

       return result.toString();
    }
}
