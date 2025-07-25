package com.vermouthx.stocker.listeners;

import com.vermouthx.stocker.entities.StockerQuote;
import com.vermouthx.stocker.settings.StockerSetting;
import com.vermouthx.stocker.utils.StockerTableModelUtil;
import com.vermouthx.stocker.views.StockerTableView;

import javax.swing.table.DefaultTableModel;
import java.util.List;

public class StockerQuoteUpdateListener implements StockerQuoteUpdateNotifier {
    private final StockerTableView myTableView;

    public StockerQuoteUpdateListener(StockerTableView myTableView) {
        this.myTableView = myTableView;
    }

    @Override
    public void syncQuotes(List<StockerQuote> quotes, int size) {
        DefaultTableModel tableModel = myTableView.getTableModel();
        StockerSetting setting = StockerSetting.Companion.getInstance();
        
        quotes.forEach(quote -> {
            quote.setShares(setting.getStockShares(quote.getCode()));
            quote.setCostBasis(setting.getStockCostBasis(quote.getCode()));
            
            synchronized (myTableView.getTableModel()) {
                int rowIndex = StockerTableModelUtil.existAt(tableModel, quote.getCode());
                if (rowIndex != -1) {
                    if (!tableModel.getValueAt(rowIndex, 1).equals(quote.getName())) {
                        tableModel.setValueAt(quote.getName(), rowIndex, 1);
                        tableModel.fireTableCellUpdated(rowIndex, 1);
                    }
                    // 更新涨幅
                    if (!tableModel.getValueAt(rowIndex, 2).equals(quote.getPercentage() + "%")) {
                        tableModel.setValueAt(quote.getPercentage() + "%", rowIndex, 2);
                        tableModel.fireTableCellUpdated(rowIndex, 2);
                    }
                    // 更新现价
                    if (!tableModel.getValueAt(rowIndex, 3).equals(quote.getCurrent())) {
                        tableModel.setValueAt(quote.getCurrent(), rowIndex, 3);
                        tableModel.fireTableCellUpdated(rowIndex, 3);
                    }
                    // 更新成本价
                    tableModel.setValueAt(quote.getCostBasis(), rowIndex, 4);
                    tableModel.fireTableCellUpdated(rowIndex, 4);
                    
                    // 更新当日盈亏
                    tableModel.setValueAt(quote.getDailyProfit(), rowIndex, 5);
                    tableModel.fireTableCellUpdated(rowIndex, 5);
                    
                    // 更新总盈亏
                    tableModel.setValueAt(quote.getTotalProfit(), rowIndex, 6);
                    tableModel.fireTableCellUpdated(rowIndex, 6);
                    
                    // 更新持股数
                    tableModel.setValueAt(quote.getShares(), rowIndex, 7);
                    tableModel.fireTableCellUpdated(rowIndex, 7);
                } else {
                    if (quotes.size() == size) {
                        tableModel.addRow(new Object[]{
                                quote.getCode(),
                                quote.getName(),
                                quote.getPercentage() + "%",
                                quote.getCurrent(),
                                quote.getCostBasis(),
                                quote.getDailyProfit(),
                                quote.getTotalProfit(),
                                quote.getShares()
                        });
                    }
                }
            }
        });
    }

    @Override
    public void syncIndices(List<StockerQuote> indices) {
        synchronized (myTableView) {
            myTableView.syncIndices(indices);
        }
    }

}
