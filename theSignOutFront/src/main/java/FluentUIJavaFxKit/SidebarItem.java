//simple helper boilerplate for sidebar items. 
package FluentUIJavaFxKit;

public record SidebarItem(String icon, String label, boolean dividerBefore) {
    public SidebarItem(String icon, String label) {
        this(icon, label, false);
    }
}
