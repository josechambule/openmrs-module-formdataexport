package org.openmrs.module.formdataexport.web.dynamic;

public class HtmlFormSectionItem {

        private Integer index;
        private String name;
        
        public HtmlFormSectionItem(Integer i, String s){
            index = i;
            name = s;
        }
        public Integer getIndex() {
            return index;
        }
        public void setIndex(Integer index) {
            this.index = index;
        }
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        
        
}
