package com.vermouthx.stocker.views.dialogs

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages  // 添加这一行
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.AlignY
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.text  // 添加这一行
import com.vermouthx.stocker.StockerAppManager
import com.vermouthx.stocker.entities.StockerQuote
import com.vermouthx.stocker.enums.StockerMarketType
import com.vermouthx.stocker.settings.StockerSetting
import com.vermouthx.stocker.utils.StockerQuoteHttpUtil
import java.awt.BorderLayout
import java.awt.event.ActionEvent
import javax.swing.*

class StockerManagementDialog(val project: Project?) : DialogWrapper(project) {

    private val setting = StockerSetting.instance

    private val tabMap: MutableMap<Int, JPanel> = mutableMapOf()

    private val currentSymbols: MutableMap<StockerMarketType, DefaultListModel<StockerQuote>> = mutableMapOf()

    private var currentMarketSelection: StockerMarketType = StockerMarketType.AShare

    init {
        title = "Manage Favorite Stocks"
        init()
    }

    override fun createCenterPanel(): DialogPanel {
        val tabbedPane = JBTabbedPane()
        tabbedPane.add("CN", createTabContent(0))
        tabbedPane.add("HK", createTabContent(1))
        tabbedPane.add("US", createTabContent(2))
//        tabbedPane.add("Crypto", createTabContent(3))
        tabbedPane.addChangeListener {
            currentMarketSelection = when (tabbedPane.selectedIndex) {
                0 -> {
                    StockerMarketType.AShare
                }

                1 -> {
                    StockerMarketType.HKStocks
                }

                2 -> {
                    StockerMarketType.USStocks
                }
//                3 -> {
//                    StockerMarketType.Crypto
//                }
                else -> return@addChangeListener
            }
        }

        val aShareListModel = DefaultListModel<StockerQuote>()
        aShareListModel.addAll(
            StockerQuoteHttpUtil.get(
                StockerMarketType.AShare, setting.quoteProvider, setting.aShareList
            )
        )
        currentSymbols[StockerMarketType.AShare] = aShareListModel
        tabMap[0]?.let { pane ->
            renderTabPane(pane, aShareListModel)
        }

        val hkStocksListModel = DefaultListModel<StockerQuote>()
        hkStocksListModel.addAll(
            StockerQuoteHttpUtil.get(
                StockerMarketType.HKStocks, setting.quoteProvider, setting.hkStocksList
            )
        )
        currentSymbols[StockerMarketType.HKStocks] = hkStocksListModel
        tabMap[1]?.let { pane ->
            renderTabPane(pane, hkStocksListModel)
        }

        val usStocksListModel = DefaultListModel<StockerQuote>()
        usStocksListModel.addAll(
            StockerQuoteHttpUtil.get(
                StockerMarketType.USStocks, setting.quoteProvider, setting.usStocksList
            )
        )
        currentSymbols[StockerMarketType.USStocks] = usStocksListModel
        tabMap[2]?.let { pane ->
            renderTabPane(pane, usStocksListModel)
        }

        tabbedPane.selectedIndex = 0
        return panel {
            row {
                cell(tabbedPane).align(AlignX.FILL)
            }
        }.withPreferredWidth(300)
    }

    override fun createActions(): Array<Action> {
        return arrayOf(
            object : OkAction() {
                override fun actionPerformed(e: ActionEvent?) {
                    val myApplication = StockerAppManager.myApplication(project)
                    if (myApplication != null) {
                        myApplication.shutdownThenClear()
                        currentSymbols[StockerMarketType.AShare]?.let { symbols ->
                            setting.aShareList = symbols.elements().asSequence().map { it.code }.toMutableList()
                        }
                        currentSymbols[StockerMarketType.HKStocks]?.let { symbols ->
                            setting.hkStocksList = symbols.elements().asSequence().map { it.code }.toMutableList()
                        }
                        currentSymbols[StockerMarketType.USStocks]?.let { symbols ->
                            setting.usStocksList = symbols.elements().asSequence().map { it.code }.toMutableList()
                        }
                        myApplication.schedule()
                    }
                    super.actionPerformed(e)
                }
            }, cancelAction
        )
    }

    private fun createTabContent(index: Int): JComponent {
        val pane = JPanel(BorderLayout())
        tabMap[index] = pane
        return panel {
            row {
                cell(pane).align(AlignX.FILL).align(AlignY.FILL)
            }
        }
    }

    private fun renderTabPane(pane: JPanel, listModel: DefaultListModel<StockerQuote>) {
        val list = JBList(listModel)
        val decorator = ToolbarDecorator.createDecorator(list)
            .setEditAction { editStockHolding(list) } // 添加编辑操作
        val toolbarPane = decorator.createPanel()
        list.installCellRenderer { symbol ->
            panel {
                row {
                    label(symbol.code).align(AlignX.LEFT)
                    label(
                        if (symbol.name.length <= 20) {
                            symbol.name
                        } else {
                            "${symbol.name.substring(0, 20)}..."
                        }
                    ).align(AlignX.CENTER)
                    // 显示持有份额和成本
                    if (symbol.shares > 0) {
                        label("持有: ${symbol.shares} 股, 成本: ${symbol.costBasis}").align(AlignX.RIGHT)
                    }
                }
            }.withBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16))
        }
        val scrollPane = JBScrollPane(list)
        pane.add(toolbarPane, BorderLayout.NORTH)
        pane.add(scrollPane, BorderLayout.CENTER)
    }
    
    // 编辑持有份额和成本的方法
    private fun editStockHolding(list: JBList<StockerQuote>) {
        val selectedQuote = list.selectedValue ?: return
        val setting = StockerSetting.instance
        
        // 创建编辑对话框
        val dialog = object : DialogWrapper(project) {
            private lateinit var sharesField: JTextField
            private lateinit var costBasisField: JTextField
            
            init {
                title = "编辑持有信息 - ${selectedQuote.code} ${selectedQuote.name}"
                init()
            }
            
            override fun createCenterPanel(): JComponent {
                return panel {
                    row("持有份额:") {
                        sharesField = textField()
                            .text(selectedQuote.shares.toString())
                            .align(AlignX.FILL)
                            .component
                    }
                    row("持有成本:") {
                        costBasisField = textField()
                            .text(selectedQuote.costBasis.toString())
                            .align(AlignX.FILL)
                            .component
                    }
                }
            }
            
            override fun doOKAction() {
                try {
                    val shares = sharesField.text.toDoubleOrNull() ?: 0.0
                    val costBasis = costBasisField.text.toDoubleOrNull() ?: 0.0
                    
                    // 更新设置
                    setting.setStockShares(selectedQuote.code, shares)
                    setting.setStockCostBasis(selectedQuote.code, costBasis)
                    
                    // 更新当前列表中的数据
                    selectedQuote.shares = shares
                    selectedQuote.costBasis = costBasis
                    list.repaint()
                    
                    super.doOKAction()
                } catch (e: Exception) {
                    Messages.showErrorDialog("请输入有效的数字", "输入错误")
                }
            }
        }
        
        dialog.show()
    }

}
