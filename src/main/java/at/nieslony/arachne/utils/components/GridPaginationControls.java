/*
 * Copyright (C) 2026 claas
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package at.nieslony.arachne.utils.components;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.select.SelectVariant;
import com.vaadin.flow.data.provider.DataProvider;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 *
 * @author claas
 */
@Slf4j
public class GridPaginationControls<T> extends VerticalLayout {

    private Grid<T> grid;
    DataProvider<T, Void> dataProvider;

    private long pageCount = 1;
    private long pageSize;
    private long currentPage = 1;
    private long totalItemCount = 0;

    Button firstPageButton;
    Button prevPageButton;
    Button nextPageButton;
    Button lastPageButton;
    Div pagePositionText;

    public GridPaginationControls(
            Grid<T> grid,
            Supplier<Long> noItems,
            Function<Pageable, Page<T>> items
    ) {
        final List<Long> pageSizes = List.of(10L, 20L, 50L, 100L, 200L, 500L);

        this.grid = grid;
        totalItemCount = noItems.get();
        pageSize = pageSizes.getFirst();
        updatePageCount();
        //pageCount = (totalItemCount - 1) / pageSize + 1;

        Select<Long> pageSizeSelect = new Select<>();
        pageSizeSelect.addThemeVariants(SelectVariant.LUMO_SMALL);
        pageSizeSelect.setWidth("5rem");
        pageSizeSelect.setItems(pageSizes);
        pageSizeSelect.setValue(pageSize);
        pageSizeSelect.addValueChangeListener(e -> {
            pageSize = e.getValue();
            updatePageCount();
            currentPage = 1;
            updateControls();
            updateItems();
            dataProvider.refreshAll();
        });

        NativeLabel pageSizeLabel = new NativeLabel("Page Size:");

        firstPageButton = createNavigationButton(
                VaadinIcon.ANGLE_DOUBLE_LEFT.create(),
                "Goto first Page",
                () -> currentPage = 1
        );
        prevPageButton = createNavigationButton(
                VaadinIcon.ANGLE_LEFT.create(),
                "Goto previous Page",
                () -> currentPage--
        );
        pagePositionText = new Div();
        nextPageButton = createNavigationButton(
                VaadinIcon.ANGLE_RIGHT.create(),
                "Goto next Page",
                () -> currentPage++
        );
        lastPageButton = createNavigationButton(
                VaadinIcon.ANGLE_DOUBLE_RIGHT.create(),
                "Goto last Page",
                () -> currentPage = pageCount
        );

        HorizontalLayout controlLayout = new HorizontalLayout();
        controlLayout.setMargin(false);
        controlLayout.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        controlLayout.addToStart(pageSizeLabel, pageSizeSelect);
        controlLayout.addToEnd(
                firstPageButton,
                prevPageButton,
                pagePositionText,
                nextPageButton,
                lastPageButton
        );
        controlLayout.setWidthFull();

        add(controlLayout);

        dataProvider = DataProvider.fromCallbacks(
                query -> {
                    query.getLimit();
                    query.getOffset();

                    Pageable pageable = PageRequest.of(
                            (int) currentPage - 1,
                            (int) pageSize
                    );
                    return items.apply(pageable).stream();
                },
                query
                -> (int) (currentPage < pageCount
                        ? pageSize
                        : (totalItemCount - 1) % pageSize + 1)
        );
        grid.setDataProvider(dataProvider);

        updateControls();
    }

    private void updatePageCount() {
        pageCount = (totalItemCount - 1) / pageSize + 1;
    }

    private Button createNavigationButton(
            Icon icon,
            String tooltip,
            Runnable onClick
    ) {
        Button button = new Button(icon);
        button.setTooltipText(tooltip);
        button.addThemeVariants(ButtonVariant.LUMO_SMALL);
        button.addClickListener(e -> {
            onClick.run();
            updateControls();
            updateItems();
            dataProvider.refreshAll();
        });

        return button;
    }

    private void updateItems() {

    }

    private void updateControls() {
        pagePositionText.setText("Page %d of %d".formatted(
                currentPage,
                pageCount
        ));
        firstPageButton.setEnabled(currentPage > 1);
        prevPageButton.setEnabled(currentPage > 1);
        nextPageButton.setEnabled(currentPage < pageCount);
        lastPageButton.setEnabled(currentPage < pageCount);
    }
}
