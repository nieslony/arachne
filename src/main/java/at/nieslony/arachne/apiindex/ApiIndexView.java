/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package at.nieslony.arachne.apiindex;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HtmlComponent;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.DescriptionList;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 *
 * @author claas
 */
@Route(value = "api-index")
@RolesAllowed("USER")
public class ApiIndexView extends VerticalLayout {

    private static final Logger logger = LoggerFactory.getLogger(ApiIndexView.class);

    public ApiIndexView(ApiIndexBean apiIndexBean) {
        H1 header = new H1("API Index");
        add(header);
        add(new H2("Table of Contents"));

        UnorderedList toc = new UnorderedList();
        add(toc);
        add(new H2("API Calls"));

        apiIndexBean.getMappings()
                .entrySet()
                .stream()
                .filter((entry) -> {
                    try {
                        String pattern = entry.getKey()
                                .getPathPatternsCondition()
                                .getFirstPattern()
                                .getPatternString();
                        return pattern.startsWith("/api");
                    } catch (NullPointerException ex) {
                        return false;
                    }
                })
                .sorted((entry1, entry2) -> {
                    var path1 = entry1.getKey()
                            .getPathPatternsCondition()
                            .getFirstPattern()
                            .getPatternString();
                    var path2 = entry2.getKey()
                            .getPathPatternsCondition()
                            .getFirstPattern()
                            .getPatternString();
                    return path1.compareTo(path2);
                })
                .forEach((entry) -> {
                    var key = entry.getKey();
                    String pattern = key.getPathPatternsCondition().getFirstPattern().getPatternString();
                    String txt = pattern + " " + key.getMethodsCondition().toString();
                    toc.add(new ListItem(txt));

                    var method = entry.getValue().getMethod();
                    Map<DescriptionList.Term, DescriptionList.Description> methodDetails = new HashMap<>();

                    var returnType = method.getReturnType();
                    if (returnType != void.class) {
                        methodDetails.put(
                                new DescriptionList.Term("Returns"),
                                new DescriptionList.Description(getJsonParams(returnType))
                        );
                    }

                    List<Div> requestParams = new LinkedList<>();
                    List<Div> pathParams = new LinkedList<>();
                    List<String> otherParams = new LinkedList<>();
                    for (var param : method.getParameters()) {
                        RequestParam requestParam = param.getDeclaredAnnotation(RequestParam.class);
                        if (requestParam != null) {
                            String name = requestParam.name();
                            String defaultValue = requestParam.defaultValue();
                            boolean isRequired = requestParam.required();

                            Div div = new Div(
                                    new Text("name: " + name),
                                    new HtmlComponent("br"),
                                    new Text("is required: " + (isRequired ? "yes" : "no")),
                                    new HtmlComponent("br"),
                                    new Text("default value: " + defaultValue)
                            );
                            requestParams.add(div);
                            continue;
                        }

                        PathVariable pathVariable = param.getDeclaredAnnotation(PathVariable.class);
                        if (pathVariable != null) {
                            String name = pathVariable.name();
                            boolean isRequired = pathVariable.required();
                            Div div = new Div(
                                    new Text("name: " + name),
                                    new HtmlComponent("br"),
                                    new Text("is required: " + (isRequired ? "yes" : "no"))
                            );
                            pathParams.add(div);
                            continue;
                        }

                        otherParams.add(param.toString() + " " + Arrays.toString(param.getDeclaredAnnotations()));
                    }

                    if (!pathParams.isEmpty()) {
                        UnorderedList l = new UnorderedList();
                        pathParams.forEach((div) -> {
                            l.add(new ListItem(div));
                        });
                        methodDetails.put(
                                new DescriptionList.Term("Path Parameters"),
                                new DescriptionList.Description(l)
                        );
                    }
                    if (!requestParams.isEmpty()) {
                        UnorderedList l = new UnorderedList();
                        requestParams.forEach((div) -> {
                            l.add(new ListItem(div));
                        });
                        methodDetails.put(
                                new DescriptionList.Term("Request Parameters"),
                                new DescriptionList.Description(l)
                        );
                    }
                    if (!otherParams.isEmpty()) {
                        UnorderedList l = new UnorderedList();
                        otherParams.forEach((div) -> {
                            l.add(new ListItem(div));
                        });
                        methodDetails.put(
                                new DescriptionList.Term("Other Parameters"),
                                new DescriptionList.Description(l)
                        );
                    }

                    add(new H3(txt));
                    add(new DescriptionList(methodDetails));
                });
    }

    private Component getJsonParams(Class<?> c) {
        if (c.isInstance(List.class)) {
            return new Text("List");
        }

        UnorderedList ul = new UnorderedList();
        for (var method : c.getMethods()) {
            if (Modifier.isPublic(method.getModifiers())) {
                String name = method.getName();
                if (name.startsWith("get")) {
                    name = name.substring(3);
                    if (method.getReturnType().isEnum()) {
                    }
                    ul.add(new ListItem(name));
                } else if (name.startsWith("is") && method.getReturnType() == boolean.class) {
                    name = name.substring(2);
                    ul.add(new ListItem(name));
                }
            }
        }

        return ul;
    }
}
