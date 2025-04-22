document.addEventListener("DOMContentLoaded", () => {
    setTimeout(() => {
        let container = document.querySelector('.container')
        container.style.opacity = 1
        container.style.filter = 'none'
    }, 100);
})

//读取展示列表
const _stor = localStorage.getItem('showList')
const showList = _stor != null ? JSON.parse(_stor) : {
    statusShowList: [
        {
            "name": "network_type",
            "isShow": true
        },
        {
            "name": "wifi_access_sta_num",
            "isShow": true
        },
        {
            "name": "battery",
            "isShow": true
        },
        {
            "name": "rssi",
            "isShow": true
        },
        {
            "name": "cpu_temp",
            "isShow": true
        },
        {
            "name": "cpu_usage",
            "isShow": true
        },
        {
            "name": "mem_usage",
            "isShow": true
        },
        {
            "name": "realtime_time",
            "isShow": true
        },
        {
            "name": "monthly_tx_bytes",
            "isShow": true
        },
        {
            "name": "daily_data",
            "isShow": true
        },
        {
            "name": "internal_available_storage",
            "isShow": true
        },
        {
            "name": "external_available_storage",
            "isShow": true
        },
        {
            "name": "realtime_rx_thrpt",
            "isShow": true
        }],
    signalShowList: [
        {
            "name": "Z5g_rsrp",
            "isShow": true
        },
        {
            "name": "Nr_snr",
            "isShow": true
        },
        {
            "name": "Nr_bands",
            "isShow": true
        },
        {
            "name": "Nr_fcn",
            "isShow": true
        },
        {
            "name": "Nr_bands_widths",
            "isShow": true
        },
        {
            "name": "Nr_pci",
            "isShow": true
        },
        {
            "name": "nr_rsrq",
            "isShow": true
        },
        {
            "name": "nr_rssi",
            "isShow": true
        },
        {
            "name": "Nr_cell_id",
            "isShow": true
        },
        {
            "name": "lte_rsrp",
            "isShow": true
        },
        {
            "name": "Lte_snr",
            "isShow": true
        },
        {
            "name": "Lte_bands",
            "isShow": true
        },
        {
            "name": "Lte_fcn",
            "isShow": true
        },
        {
            "name": "Lte_bands_widths",
            "isShow": true
        },
        {
            "name": "Lte_pci",
            "isShow": true
        },
        {
            "name": "lte_rsrq",
            "isShow": true
        },
        {
            "name": "lte_rssi",
            "isShow": true
        },
        {
            "name": "Lte_cell_id",
            "isShow": true
        }],
    propsShowList: [
        {
            "name": "model",
            "isShow": true
        },
        {
            "name": "cr_version",
            "isShow": true
        },
        {
            "name": "iccid",
            "isShow": true
        },
        {
            "name": "imei",
            "isShow": true
        },
        {
            "name": "imsi",
            "isShow": true
        },
        {
            "name": "ipv6_wan_ipaddr",
            "isShow": true
        },
        {
            "name": "lan_ipaddr",
            "isShow": true
        },
        {
            "name": "mac_address",
            "isShow": true
        },
        {
            "name": "msisdn",
            "isShow": true
        }
    ]

}

let smsSender = null

// #拖动管理 list为当前最新正确顺序
const saveDragListData = (list, callback) => {
    //拖动状态更改
    const children = Array.from(list.querySelectorAll('input'))
    let id = null
    if (list.id == 'draggable_status') id = 'statusShowList'
    if (list.id == 'draggable_signal') id = 'signalShowList'
    if (list.id == 'draggable_props') id = 'propsShowList'
    if (!id) return
    //遍历
    showList[id] = children.map((item) => ({
        name: item.dataset.name,
        isShow: item.checked
    }))
    localStorage.setItem('showList', JSON.stringify(showList))
    //保存
    callback && callback(list)
}

//初始化drag触发器
DragList("#draggable_status", (list) => saveDragListData(list, (d_list) => {
    localStorage.setItem('statusShowListDOM', d_list.innerHTML)
}))
DragList("#draggable_signal", (list) => saveDragListData(list, (d_list) => {
    localStorage.setItem('signalShowListDOM', d_list.innerHTML)
}))
DragList("#draggable_props", (list) => saveDragListData(list, (d_list) => {
    localStorage.setItem('propsShowListDOM', d_list.innerHTML)
}))

//渲染listDOM
const listDOM_STATUS = document.querySelector("#draggable_status")
const listDOM_SIGNAL = document.querySelector("#draggable_signal")
const listDOM_PROPS = document.querySelector("#draggable_props")
const statusDOMStor = localStorage.getItem('statusShowListDOM')
const signalDOMStor = localStorage.getItem('signalShowListDOM')
const propsDOMStor = localStorage.getItem('propsShowListDOM')
statusDOMStor && (listDOM_STATUS.innerHTML = statusDOMStor)
signalDOMStor && (listDOM_SIGNAL.innerHTML = signalDOMStor)
propsDOMStor && (listDOM_PROPS.innerHTML = propsDOMStor)

//按照showList初始化排序模态框
listDOM_STATUS.querySelectorAll('input').forEach((item) => {
    let name = item.dataset.name
    let foundItem = showList.statusShowList.find(i => i.name == name)
    if (foundItem) {
        item.checked = foundItem.isShow
    }
})
listDOM_SIGNAL.querySelectorAll('input').forEach((item) => {
    let name = item.dataset.name
    let foundItem = showList.signalShowList.find(i => i.name == name)
    if (foundItem) {
        item.checked = foundItem.isShow
    }
})
listDOM_PROPS.querySelectorAll('input').forEach((item) => {
    let name = item.dataset.name
    let foundItem = showList.propsShowList.find(i => i.name == name)
    if (foundItem) {
        item.checked = foundItem.isShow
    }
})
const isNullOrUndefiend = (obj) => {
    let isNumber = typeof obj === 'number'
    if (isNumber) {
        //如果是数字类型，直接返回
        return true
    }
    return obj != undefined || obj != null
}

//如果是数字类型，直接返回
let isIncludeInShowList = (dicName) => (
    showList.statusShowList.find(i => i.name == dicName)
    || showList.propsShowList.find(i => i.name == dicName)
    || showList.signalShowList.find(i => i.name == dicName)
)

function notNullOrundefinedOrIsShow(obj, dicName, flag = false) {
    let isNumber = typeof obj[dicName] === 'number'
    if (isNumber) {

        return isIncludeInShowList(dicName) || flag
    }
    let isReadable = obj[dicName] != null && obj[dicName] != undefined && obj[dicName] != ''
    //这里需要遍历一下是否显示的字段
    return isReadable && isIncludeInShowList(dicName)
}

const onTokenConfirm = debounce(async () => {
    try {
        let tkInput = document.querySelector('#tokenInput')
        let password = tkInput && (tkInput.value)
        if (!password || !password?.trim()) return createToast('请输入密码！', 'red')
        KANO_PASSWORD = password.trim()
        const cookie = await login()
        if (!cookie) {
            createToast(`登录失败，请检查密码和网络！`, 'red')
            tkInput.value = ''
            out()
            return null
        }
        createToast('登录成功！', 'green')
        localStorage.setItem('kano_sms_pwd', password.trim())

        closeModal('#tokenModal')
        handlerADBStatus()
        handlerADBNetworkStatus()
        handlerPerformaceStatus()
        initNetworktype()
        initSMBStatus()
        initLightStatus()
        initBandForm()
        initUSBNetworkType()
        initWIFISwitch()
        rebootDeviceBtnInit()
        handlerCecullarStatus()
        initScheduleRebootStatus()
        initShutdownBtn()
        initATBtn()
        initChangePassData()
        initSimCardType()
    }
    catch {
        createToast('登录失败，请检查网络！', 'red')
    }
}, 200)

let timer_out = null
function out() {
    smsSender && smsSender()
    localStorage.removeItem('kano_sms_pwd')
    closeModal('#smsList')
    clearTimeout(timer_out)
    timer_out = setTimeout(() => {
        showModal('#tokenModal')
    }, 320);
}

let initRequestData = () => {
    const PWD = localStorage.getItem('kano_sms_pwd')
    if (!PWD) {
        return false
    }
    KANO_PASSWORD = PWD
    return true
}

let getSms = async () => {
    if (!initRequestData()) {
        out()
        return null
    }
    try {
        let res = await getSmsInfo()
        if (!res) {
            out()
            createToast(res.error, 'red')
            return null
        }
        return res.messages
    } catch {
        out()
        return null
    }
}

let isDisabledSendSMS = false
let sendSMS = async () => {
    const SMSInput = document.querySelector('#SMSInput')
    const PhoneInput = document.querySelector('#PhoneInput')
    if (SMSInput && SMSInput.value && SMSInput.value.trim()
        && PhoneInput && PhoneInput.value && Number(PhoneInput.value.trim())
    ) {
        try {
            if (isDisabledSendSMS) return createToast('请不要频繁发送！', 'red')
            const content = SMSInput.value.trim()
            const number = PhoneInput.value.trim()
            isDisabledSendSMS = true
            const res = await sendSms_UFI({ content, number })
            if (res && res.result == 'success') {
                SMSInput.value = ''
                createToast('发送成功！', 'green')
                handleSmsRender()
            } else {
                createToast((res && res.message) ? res.message : '发送失败', 'red')
            }
        } catch {
            createToast('发送失败，请检查网络和密码', 'red')
            out()
        }
        isDisabledSendSMS = false
    } else {
        createToast('请输入手机号和内容', 'red')
    }
}

const deleteState = new Map();
const deleteSMS = async (id) => {
    const message = document.querySelector(`#message${id}`);

    if (!message) return;

    // 获取当前 id 的删除状态
    let state = deleteState.get(id) || { confirmCount: 0, timer: null, isDeleting: false };

    if (state.isDeleting) return; // 正在删除时禁止操作

    state.confirmCount += 1;
    message.style.display = '';

    // 清除之前的计时器，重新设置 2 秒后重置状态
    clearTimeout(state.timer);
    state.timer = setTimeout(() => {
        state.confirmCount = 0;
        message.style.display = 'none';
        deleteState.set(id, state);
    }, 2000);

    deleteState.set(id, state);

    if (state.confirmCount < 2) return; // 第一次点击时仅提示

    // 进入删除状态，防止重复点击
    state.isDeleting = true;
    deleteState.set(id, state);

    try {
        const res = await removeSmsById(id);
        if (res?.result === 'success') {
            createToast('删除成功！', 'green');
            handleSmsRender();
        } else {
            createToast(res?.message || '删除失败', 'red');
        }
    } catch {
        createToast('操作失败，请检查网络和密码', 'red');
    }

    // 删除完成后，清理状态
    deleteState.delete(id);
};

let isFirstRender = true
let lastRequestSmsIds = null
let handleSmsRender = async () => {
    let list = document.querySelector('#sms-list')
    if (!list) createToast('没有找到短信列表节点', 'red')
    if (isFirstRender) {
        list.innerHTML = ` <li><h2 style="padding: 30px;text-align:center;height:100vh">Loading...</h2></li>`
    }
    isFirstRender = false
    showModal('#smsList')
    let res = await getSms()
    if (res && res.length) {
        //防止重复渲染
        let ids = res.map(item => item.id).join('')
        if (ids === lastRequestSmsIds) return
        lastRequestSmsIds = ids
        const dateStrArr = ['年', '月', '日', ':', ':', '']
        res.sort((a, b) => {
            let date_a = a.date.split(',')
            let date_b = b.date.split(',')
            date_a.pop()
            date_b.pop()
            return Number(date_b.join('')) - Number(date_a.join(''))
        })
        list.innerHTML = res.map(item => {
            let date = item.date.split(',')
            date.pop()
            date = date.map((item, index) => {
                return item + dateStrArr[index]
            }).join('')
            return `<li class="sms-item" style="${item.tag != '2' ? 'background-color:#0880001f;margin-left:15px' : 'background-color:#ffc0cb63;margin-right:15px'}">
                                        <div class="arrow" style="${item.tag == '2' ? 'right:-30px;border-color: transparent transparent transparent #ffc0cb63' : 'left:-30px;border-color: transparent #0880001f transparent transparent'}"></div>
                                        <div class="icon" onclick="deleteSMS(${item.id})">
                                            <span id="message${item.id}" style="display:none;color:red;position: absolute;width: 100px;top: 6px;right: 20px;">确定要删除吗？</span>
                                            <svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" t="1742373390977" class="icon" viewBox="0 0 1024 1024" version="1.1" p-id="2837" width="16" height="16"><path d="M848 144H608V96a48 48 0 0 0-48-48h-96a48 48 0 0 0-48 48v48H176a48 48 0 0 0-48 48v48h768v-48a48 48 0 0 0-48-48zM176 928a48 48 0 0 0 48 48h576a48 48 0 0 0 48-48V288H176v640z m480-496a48 48 0 1 1 96 0v400a48 48 0 1 1-96 0V432z m-192 0a48 48 0 1 1 96 0v400a48 48 0 1 1-96 0V432z m-192 0a48 48 0 1 1 96 0v400a48 48 0 1 1-96 0V432z" fill="" p-id="2838"/></svg>
                                        </div>
                                        <p style="color:#adadad;font-size:16px;margin:4px 0">${item.number}</p>
                                        <p>${decodeBase64(item.content)}</p>
                                        <p style="text-align:right;color:#adadad;margin-top:4px">${date}</p >
                                    </li > `
        }).join('')
    } else {
        if (!res) {
            out()
        }
        list.innerHTML = ` <li> <h2 style="padding: 30px;text-align:center;">没有短信</h2></li >`
    }
}


let StopStatusRenderTimer = null
let handlerStatusRender = async (flag = false) => {
    const status = document.querySelector('#STATUS')
    if (flag) {
        status.innerHTML = `
        <li style="padding-top: 15px;">
            <strong class="green" style="margin: 10px auto;margin-top: 0; display: flex;flex-direction: column;padding: 40px;">
                <span style="font-size: 50px;" class="spin">🌀</span>
                <span style="font-size: 16px;padding-top: 10px;">loading...</span>
            </strong>
        </li>`
    }
    const res = await getUFIData()
    if (!res) {
        // out()
        if (flag) {
            status.innerHTML = `<li style="padding-top: 15px;"><strong onclick="copyText(event)" class="green">当你看到这个tag的时候，请检查你的网络连接与软件内网关地址是否正确~</strong></li>`
            createToast('获取数据失败，请检查网络！', 'red')
            setTimeout(() => {
                try {
                    KANO_INTERFACE && KANO_INTERFACE.exit()
                } catch { }
            }, 3000);
        }
        return
    }
    if (res) {
        const current_cell = document.querySelector('#CURRENT_CELL')
        let html = ''

        if (current_cell) {
            current_cell.innerHTML = '<i>当前连接</i><br/>'
            current_cell.innerHTML += `
            ${notNullOrundefinedOrIsShow(res, 'Lte_fcn') ? `<span>频率: ${res.Lte_fcn}</span>` : ''}
            ${notNullOrundefinedOrIsShow(res, 'Lte_pci') ? `<span>&nbsp;PCI: ${res.Lte_pci}</span>` : ''}
            ${notNullOrundefinedOrIsShow(res, 'lte_rsrp') ? `<div style="display: flex;padding-bottom:2px;align-items: center;">RSRP:&nbsp; ${kano_parseSignalBar(res.lte_rsrp)}</div>` : ''}
            ${notNullOrundefinedOrIsShow(res, 'Lte_snr') ? `<div style="display: flex;align-items: center;">SINR:&nbsp; ${kano_parseSignalBar(res.Lte_snr, -10, 30, 13, 0)}</div>` : ''}
            ${notNullOrundefinedOrIsShow(res, 'lte_rsrq') ? `<div style="display: flex;padding-top:2px;align-items: center;">RSRQ:&nbsp; ${kano_parseSignalBar(res.lte_rsrq, -20, -3, -9, -12)}</div>` : ''}
            ${notNullOrundefinedOrIsShow(res, 'Nr_fcn') ? `<span>频率: ${res.Nr_fcn}</span>` : ''}
            ${notNullOrundefinedOrIsShow(res, 'Nr_pci') ? `<span>&nbsp;PCI: ${res.Nr_pci}</span>` : ''}
            ${notNullOrundefinedOrIsShow(res, 'Z5g_rsrp') ? `<div style="display: flex;padding-bottom:2px;align-items: center;width: 114px;justify-content: space-between"><span>RSRP:</span>${kano_parseSignalBar(res.Z5g_rsrp)}</div>` : ''}
            ${notNullOrundefinedOrIsShow(res, 'Nr_snr') ? `<div style="display: flex;align-items: center;width: 114px;justify-content: space-between"><span>SINR:</span>${kano_parseSignalBar(res.Nr_snr, -10, 30, 13, 0)}</div>` : ''}
            ${notNullOrundefinedOrIsShow(res, 'nr_rsrq') ? `<div style="display: flex;padding-top:2px;align-items: center;width: 114px;justify-content: space-between"><span>RSRQ:</span>${kano_parseSignalBar(res.nr_rsrq, -20, -3, -9, -12)}</div>` : ''}
            `
        }

        let statusHtml_base = {
            network_type: `${notNullOrundefinedOrIsShow(res, 'network_type') ? `<strong onclick="copyText(event)"  class="green">蜂窝状态：${res.network_provider} ${res.network_type == '20' ? '5G' : res.network_type == '13' ? '4G' : res.network_type}</strong>` : ''}`,
            wifi_access_sta_num: `${notNullOrundefinedOrIsShow(res, 'wifi_access_sta_num') ? `<strong onclick="copyText(event)"  class="blue">WIFI设备数：${res.wifi_access_sta_num}</strong>` : ''}`,
            battery: `${notNullOrundefinedOrIsShow(res, 'battery') ? `<strong onclick="copyText(event)"  class="green">剩余电量：${res.battery} %</strong>` : ''}`,
            rssi: `${notNullOrundefinedOrIsShow(res, 'rssi') || notNullOrundefinedOrIsShow(res, 'network_signalbar', true) ? `<strong onclick="copyText(event)"  class="green">蜂窝信号强度：${kano_getSignalEmoji(notNullOrundefinedOrIsShow(res, 'rssi') ? res.rssi : res.network_signalbar)}</strong>` : ''}`,
            cpu_temp: `${notNullOrundefinedOrIsShow(res, 'cpu_temp') ? `<strong onclick="copyText(event)"  class="blue">CPU温度：${Number(res.cpu_temp / 1000).toFixed(2)} ℃</strong>` : ''}`,
            cpu_usage: `${notNullOrundefinedOrIsShow(res, 'cpu_usage') ? `<strong onclick="copyText(event)"  class="blue">CPU使用率：${Number(res.cpu_usage).toFixed(2)} %</strong>` : ''}`,
            mem_usage: `${notNullOrundefinedOrIsShow(res, 'mem_usage') ? `<strong onclick="copyText(event)"  class="blue">内存使用率：${Number(res.mem_usage).toFixed(2)} %</strong>` : ''}`,
            realtime_time: `${notNullOrundefinedOrIsShow(res, 'realtime_time') ? `<strong onclick="copyText(event)"  class="blue">连接时长：${kano_formatTime(Number(res.realtime_time))}${res.monthly_time ? '&nbsp;<span style="color:white">/</span>&nbsp;总时长(月): ' + kano_formatTime(Number(res.monthly_time)) : ''}</strong>` : ''}`,
            monthly_tx_bytes: `${notNullOrundefinedOrIsShow(res, 'monthly_tx_bytes') || notNullOrundefinedOrIsShow(res, 'monthly_rx_bytes') ? `<strong onclick="copyText(event)"  class="blue">已用流量：<span class="red">${formatBytes(Number((res.monthly_tx_bytes + res.monthly_rx_bytes)))}</span>${(res.data_volume_limit_size || res.flux_data_volume_limit_size) ? '&nbsp;<span style="color:white">/</span>&nbsp;总流量：' + formatBytes((() => {
                const limit_size = res.data_volume_limit_size ? res.data_volume_limit_size : res.flux_data_volume_limit_size
                if (!limit_size) return ''
                return limit_size.split('_')[0] * limit_size.split('_')[1] * Math.pow(1024, 2)
            })()) : ''}</strong>` : ''}`,
            daily_data: `${notNullOrundefinedOrIsShow(res, 'daily_data') ? `<strong onclick="copyText(event)"  class="blue">当日流量：${formatBytes(res.daily_data)}</strong>` : ''}`,
            internal_available_storage: `${notNullOrundefinedOrIsShow(res, 'internal_available_storage') || notNullOrundefinedOrIsShow(res, 'internal_total_storage') ? `<strong onclick="copyText(event)" class="blue">内部存储：${formatBytes(res.internal_used_storage)} 已用 / ${formatBytes(res.internal_total_storage)} 总容量</strong>` : ''}`,
            external_available_storage: `${notNullOrundefinedOrIsShow(res, 'external_available_storage') || notNullOrundefinedOrIsShow(res, 'external_total_storage') ? `<strong onclick="copyText(event)" class="blue">SD卡：${formatBytes(res.external_used_storage)} 已用 / ${formatBytes(res.external_total_storage)} 总容量</strong>` : ''}`,
            realtime_rx_thrpt: `${notNullOrundefinedOrIsShow(res, 'realtime_tx_thrpt') || notNullOrundefinedOrIsShow(res, 'realtime_rx_thrpt') ? `<strong onclick="copyText(event)" class="blue">当前网速: ⬇️ ${formatBytes(Number((res.realtime_rx_thrpt)))}/S ⬆️ ${formatBytes(Number((res.realtime_tx_thrpt)))}/S</strong>` : ''}`,
        }
        let statusHtml_net = {
            lte_rsrp: `${notNullOrundefinedOrIsShow(res, 'lte_rsrp') ? `<strong onclick="copyText(event)"  class="green">4G接收功率：${kano_parseSignalBar(res.lte_rsrp)}</strong>` : ''}`,
            Lte_snr: `${notNullOrundefinedOrIsShow(res, 'Lte_snr') ? `<strong onclick="copyText(event)"  class="blue">4G SINR：${kano_parseSignalBar(res.Lte_snr, -10, 30, 13, 0)}</strong>` : ''}`,
            Lte_bands: `${notNullOrundefinedOrIsShow(res, 'Lte_bands') ? `<strong onclick="copyText(event)"  class="blue">4G 注册频段：B${res.Lte_bands}</strong>` : ''}`,
            Lte_fcn: `${notNullOrundefinedOrIsShow(res, 'Lte_fcn') ? `<strong onclick="copyText(event)"  class="green">4G 频率：${res.Lte_fcn}</strong>` : ''}`,
            Lte_bands_widths: `${notNullOrundefinedOrIsShow(res, 'Lte_bands_widths') ? `<strong onclick="copyText(event)"  class="green">4G 频宽：${res.Lte_bands_widths}</strong>` : ''}`,
            Lte_pci: `${notNullOrundefinedOrIsShow(res, 'Lte_pci') ? `<strong onclick="copyText(event)"  class="blue">4G PCI：${res.Lte_pci}</strong>` : ''}`,
            lte_rsrq: `${notNullOrundefinedOrIsShow(res, 'lte_rsrq') ? `<strong onclick="copyText(event)"  class="blue">4G RSRQ：${kano_parseSignalBar(res.lte_rsrq, -20, -3, -9, -12)}</strong>` : ''}`,
            lte_rssi: `${notNullOrundefinedOrIsShow(res, 'lte_rssi') ? `<strong onclick="copyText(event)"  class="green">4G RSSI：${res.lte_rssi}</strong>` : ''}`,
            Lte_cell_id: `${notNullOrundefinedOrIsShow(res, 'Lte_cell_id') ? `<strong onclick="copyText(event)"  class="green">4G 小区ID：${res.Lte_cell_id}</strong>` : ''}`,
            Z5g_rsrp: `${notNullOrundefinedOrIsShow(res, 'Z5g_rsrp') ? `<strong onclick="copyText(event)"  class="green">5G接收功率：${kano_parseSignalBar(res.Z5g_rsrp)}</strong>` : ''}`,
            Nr_snr: `${notNullOrundefinedOrIsShow(res, 'Nr_snr') ? `<strong onclick="copyText(event)"  class="green">5G SINR：${kano_parseSignalBar(res.Nr_snr, -10, 30, 13, 0)}</strong>` : ''}`,
            Nr_bands: `${notNullOrundefinedOrIsShow(res, 'Nr_bands') ? `<strong onclick="copyText(event)"  class="green">5G 注册频段：N${res.Nr_bands}</strong>` : ''}`,
            Nr_fcn: `${notNullOrundefinedOrIsShow(res, 'Nr_fcn') ? `<strong onclick="copyText(event)"  class="blue">5G 频率：${res.Nr_fcn}</strong>` : ''}`,
            Nr_bands_widths: `${notNullOrundefinedOrIsShow(res, 'Nr_bands_widths') ? `<strong onclick="copyText(event)"  class="blue">5G 频宽：${res.Nr_bands_widths}</strong>` : ''}`,
            Nr_pci: `${notNullOrundefinedOrIsShow(res, 'Nr_pci') ? `<strong onclick="copyText(event)"  class="green">5G PCI：${res.Nr_pci}</strong>` : ''}`,
            nr_rsrq: `${notNullOrundefinedOrIsShow(res, 'nr_rsrq') ? `<strong onclick="copyText(event)"  class="green">5G RSRQ：${kano_parseSignalBar(res.nr_rsrq, -20, -3, -9, -12)}</strong>` : ''}`,
            nr_rssi: `${notNullOrundefinedOrIsShow(res, 'nr_rssi') ? `<strong onclick="copyText(event)"  class="blue">5G RSSI：${res.nr_rssi}</strong>` : ''}`,
            Nr_cell_id: `${notNullOrundefinedOrIsShow(res, 'Nr_cell_id') ? `<strong onclick="copyText(event)"  class="blue">5G 小区ID：${res.Nr_cell_id}</strong>` : ''}`,
        }

        let statusHtml_other = {
            model: `${notNullOrundefinedOrIsShow(res, 'model') ? `<strong onclick="copyText(event)"  class="blue">设备型号：${res.model}</strong>` : ''}`,
            cr_version: `${notNullOrundefinedOrIsShow(res, 'cr_version') ? `<strong onclick="copyText(event)"  class="blue">版本号：${res.cr_version}</strong>` : ''}`,
            iccid: `${notNullOrundefinedOrIsShow(res, 'iccid') ? `<strong onclick="copyText(event)"  class="blue">ICCID：${res.iccid}</strong>` : ''}`,
            imei: `${notNullOrundefinedOrIsShow(res, 'imei') ? `<strong onclick="copyText(event)"  class="blue">IMEI：${res.imei}</strong>` : ''}`,
            imsi: `${notNullOrundefinedOrIsShow(res, 'imsi') ? `<strong onclick="copyText(event)"  class="blue">IMSI：${res.imsi}</strong>` : ''}`,
            ipv6_wan_ipaddr: `${notNullOrundefinedOrIsShow(res, 'ipv6_wan_ipaddr') ? `<strong onclick="copyText(event)"  class="blue">IPV6地址：${res.ipv6_wan_ipaddr}</strong>` : ''}`,
            lan_ipaddr: `${notNullOrundefinedOrIsShow(res, 'lan_ipaddr') ? `<strong onclick="copyText(event)"  class="blue">本地网关：${res.lan_ipaddr}</strong>` : ''}`,
            mac_address: `${notNullOrundefinedOrIsShow(res, 'mac_address') ? `<strong onclick="copyText(event)"  class="blue">MAC地址：${res.mac_address}</strong>` : ''}`,
            msisdn: `${notNullOrundefinedOrIsShow(res, 'msisdn') ? `<strong onclick="copyText(event)"  class="blue">手机号：${res.msisdn}</strong>` : ''}`,
        }

        html += `<li style="padding-top: 15px;"><p>`
        showList.statusShowList.forEach(item => {
            if (statusHtml_base[item.name] && item.isShow) {
                html += statusHtml_base[item.name]
            }
        })
        html += `</p></li>`
        html += `<div class="title" style="margin: 6px 0;"><b>信号参数</b></div>`

        html += `<li style="padding-top: 15px;"><p>`
        showList.signalShowList.forEach(item => {
            if (statusHtml_net[item.name] && item.isShow) {
                html += statusHtml_net[item.name]
            }
        })
        html += `</p></li>`
        html += `<div class="title" style="margin: 6px 0;"><b>设备属性</b></div>`

        html += `<li style="padding-top: 15px;"><p>`
        showList.propsShowList.forEach(item => {
            if (statusHtml_other[item.name] && item.isShow) {
                html += statusHtml_other[item.name]
            }
        })
        html += `</p></li>`
        status && (status.innerHTML = html)
    }
}
handlerStatusRender(true)
StopStatusRenderTimer = requestInterval(() => handlerStatusRender(), 800)

//检查usb调试状态
let handlerADBStatus = async () => {
    const btn = document.querySelector('#ADB')
    if (!initRequestData()) {
        btn.onclick = () => createToast('请登录', 'red')
        btn.style.backgroundColor = '#80808073'
        return null
    }
    let res = await getData(new URLSearchParams({
        cmd: 'usb_port_switch'
    }))
    btn.onclick = async () => {
        try {
            if (!initRequestData()) {
                return null
            }
            const cookie = await login()
            if (!cookie) {
                createToast('登录失败，请检查密码', 'red')
                out()
                return null
            }
            let res1 = await (await postData(cookie, {
                goformId: 'USB_PORT_SETTING',
                usb_port_switch: res.usb_port_switch == '1' ? '0' : '1'
            })).json()

            if (res1.result == 'success') {
                createToast('操作成功！', 'green')
                await handlerADBStatus()
            } else {
                createToast('操作失败！', 'red')
            }
        } catch (e) {
            console.error(e.message)
        }
    }
    btn.innerHTML = res.usb_port_switch == '1' ? '关闭USB调试' : '开启USB调试'
    btn.style.backgroundColor = res.usb_port_switch == '1' ? '#018ad8b0' : ''

}
handlerADBStatus()

//检查usb网络调试状态
let handlerADBNetworkStatus = async () => {
    const btn = document.querySelector('#ADB_NET')
    if (!initRequestData()) {
        btn.onclick = () => createToast('请登录', 'red')
        btn.style.backgroundColor = '#80808073'
        return null
    }

    let res = await (await fetch(`${KANO_baseURL}/adb_wifi_setting`, {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json'
        }
    })).json()

    btn.onclick = async () => {
        try {
            if (!initRequestData()) {
                return null
            }
            const cookie = await login()
            if (!cookie) {
                createToast('登录失败，请检查密码', 'red')
                out()
                return null
            }
            let res1 = await (await fetch(`${KANO_baseURL}/adb_wifi_setting`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    enabled: res.enabled == "true" || res.enabled == true ? false : true,
                    password: KANO_PASSWORD
                })
            })).json()
            if (res1.result == 'success') {
                createToast('操作成功！重启生效', 'green')
                await handlerADBNetworkStatus()
            } else {
                createToast('操作失败！', 'red')
            }
        } catch (e) {
            console.error(e.message)
        }
    }
    btn.innerHTML = res.enabled == "true" || res.enabled == true ? '关闭网络ADB自启' : '开启网络ADB自启'
    btn.style.backgroundColor = res.enabled == "true" || res.enabled == true ? '#018ad8b0' : ''

}
handlerADBNetworkStatus()

//检查性能模式状态
let handlerPerformaceStatus = async () => {
    const btn = document.querySelector('#PERF')
    if (!initRequestData()) {
        btn.onclick = () => createToast('请登录', 'red')
        btn.style.backgroundColor = '#80808073'
        return null
    }
    let res = await getData(new URLSearchParams({
        cmd: 'performance_mode'
    }))
    btn.innerHTML = res.performance_mode == '1' ? '关闭性能模式' : '开启性能模式'
    btn.style.backgroundColor = res.performance_mode == '1' ? '#018ad8b0' : ''
    btn.onclick = async () => {
        try {
            if (!initRequestData()) {
                return null
            }
            const cookie = await login()
            if (!cookie) {
                createToast('登录失败，请检查密码', 'red')
                out()
                return null
            }
            let res1 = await (await postData(cookie, {
                goformId: 'PERFORMANCE_MODE_SETTING',
                performance_mode: res.performance_mode == '1' ? '0' : '1'
            })).json()
            if (res1.result == 'success') {
                createToast('操作成功，重启生效！', 'green')
                await handlerPerformaceStatus()
            } else {
                createToast('操作失败！', 'red')
            }
        } catch (e) {
            // createToast(e.message)
        }
    }
}
handlerPerformaceStatus()

function init() {
    smsSender && smsSender()
    if (!localStorage.getItem('kano_sms_pwd')) {
        showModal('#tokenModal')
    } else {
        isFirstRender = true
        lastRequestSmsIds = null
        handleSmsRender()
        smsSender = requestInterval(() => handleSmsRender(), 2000)
    }
}

// init()
let smsBtn = document.querySelector('#SMS')
smsBtn.onclick = init

let clearBtn = document.querySelector('#CLEAR')
clearBtn.onclick = () => {
    isFirstRender = true
    lastRequestSmsIds = null
    localStorage.removeItem('kano_sms_pwd')
    handlerADBStatus()
    handlerADBNetworkStatus()
    handlerPerformaceStatus()
    initNetworktype()
    initSMBStatus()
    initLightStatus()
    initBandForm()
    rebootDeviceBtnInit()
    initUSBNetworkType()
    initWIFISwitch()
    handlerCecullarStatus()
    initScheduleRebootStatus()
    initShutdownBtn()
    initATBtn()
    initChangePassData()
    initSimCardType()
    //退出登录请求
    try {
        login().then(cookie => {
            logout(cookie)
        })
    } catch { }
    createToast('您已退出登录', 'green')
    showModal('#tokenModal')
}

let initNetworktype = async () => {
    const selectEl = document.querySelector('#NET_TYPE')
    if (!initRequestData() || !selectEl) {
        selectEl.style.backgroundColor = '#80808073'
        selectEl.disabled = true
        return null
    }
    selectEl.style.backgroundColor = ''
    selectEl.disabled = false
    let res = await getData(new URLSearchParams({
        cmd: 'net_select'
    }))
    if (!selectEl || !res || res.net_select == null || res.net_select == undefined) {
        return
    }

    [...selectEl.children].forEach((item) => {
        if (item.value == res.net_select) {
            item.selected = true
        }
    })
}
initNetworktype()

const changeNetwork = async (e) => {
    const value = e.target.value.trim()
    if (!initRequestData() || !value) {
        return null
    }
    createToast('更改中，请稍后', '#BF723F')
    try {
        const cookie = await login()
        if (!cookie) {
            createToast('登录失败，请检查密码', 'red')
            out()
            return null
        }
        let res = await (await postData(cookie, {
            goformId: 'SET_BEARER_PREFERENCE',
            BearerPreference: value.trim()
        })).json()
        if (res.result == 'success') {
            createToast('操作成功！', 'green')
        } else {
            createToast('操作失败！', 'red')
        }
        await initNetworktype()
    } catch (e) {
        // createToast(e.message)
    }
}

let initUSBNetworkType = async () => {
    const selectEl = document.querySelector('#USB_TYPE')
    if (!initRequestData() || !selectEl) {
        selectEl.style.backgroundColor = '#80808073'
        selectEl.disabled = true
        return null
    }
    selectEl.style.backgroundColor = ''
    selectEl.disabled = false
    let res = await getData(new URLSearchParams({
        cmd: 'usb_network_protocal'
    }))
    if (!selectEl || !res || res.usb_network_protocal == null || res.usb_network_protocal == undefined) {
        return
    }
    [...selectEl.children].forEach((item) => {
        if (item.value == res.usb_network_protocal) {
            item.selected = true
        }
    })
}
initUSBNetworkType()

let changeUSBNetwork = async (e) => {
    const value = e.target.value.trim()
    if (!initRequestData() || !value) {
        return null
    }
    createToast('更改中，请稍后', '#BF723F')
    try {
        const cookie = await login()
        if (!cookie) {
            createToast('登录失败，请检查密码', 'red')
            out()
            return null
        }
        let res = await (await postData(cookie, {
            goformId: 'SET_USB_NETWORK_PROTOCAL',
            usb_network_protocal: value.trim()
        })).json()
        if (res.result == 'success') {
            createToast('操作成功，请重启设备生效！', 'green')
        } else {
            createToast('操作失败！', 'red')
        }
        await initUSBNetworkType()
    } catch (e) {
        // createToast(e.message)
    }
}

//WiFi开关切换_INIT
let initWIFISwitch = async () => {
    const selectEl = document.querySelector('#WIFI_SWITCH')
    if (!initRequestData() || !selectEl) {
        selectEl.style.backgroundColor = '#80808073'
        selectEl.disabled = true
        return null
    }

    selectEl.style.backgroundColor = ''
    selectEl.disabled = false
    let { WiFiModuleSwitch, ResponseList } = await getData(new URLSearchParams({
        cmd: 'queryWiFiModuleSwitch,queryAccessPointInfo'
    }))

    if (WiFiModuleSwitch == "1") {
        if (ResponseList?.length) {
            ResponseList.forEach(item => {
                if (item.AccessPointSwitchStatus == '1') {
                    selectEl.value = item.ChipIndex == "0" ? 'chip1' : 'chip2'
                }
            })
        }
    } else {
        selectEl.value = 0
    }
}
initWIFISwitch()

//WiFi开关切换
let changeWIFISwitch = async (e) => {
    const value = e.target.value.trim()
    if (!initRequestData() || !value) {
        createToast('需要登录', 'red')
        return null
    }
    createToast('更改中，请稍后', '#BF723F')
    try {
        const cookie = await login()
        if (!cookie) {
            createToast('登录失败，请检查密码', 'red')
            out()
            return null
        }
        let res = null
        if (value == "0" || value == 0) {
            res = await (await postData(cookie, {
                goformId: 'switchWiFiModule',
                SwitchOption: 0
            })).json()
        } else if (value == 'chip1' || value == 'chip2') {
            res = await (await postData(cookie, {
                goformId: 'switchWiFiChip',
                ChipEnum: value,
                GuestEnable: 0
            })).json()
        } else {
            return
        }
        if (res.result == 'success') {
            createToast('操作成功，请重新连接WiFi！', 'green')
            closeModal("#WIFIManagementModal")
        } else {
            createToast('操作失败！', 'red')
        }
        await initWIFISwitch()
    } catch (e) {
        // createToast(e.message)
    }
}

let initSMBStatus = async () => {
    const el = document.querySelector('#SMB')
    if (!initRequestData() || !el) {
        el.onclick = () => createToast('请登录', 'red')
        el.style.backgroundColor = '#80808073'
        return null
    }
    let res = await getData(new URLSearchParams({
        cmd: 'samba_switch'
    }))
    if (!el || !res || res.samba_switch == null || res.samba_switch == undefined) return
    el.onclick = async () => {
        if (!initRequestData()) {
            return null
        }
        try {
            const cookie = await login()
            if (!cookie) {
                createToast('登录失败，请检查密码', 'red')
                out()
                return null
            }
            let res1 = await (await postData(cookie, {
                goformId: 'SAMBA_SETTING',
                samba_switch: res.samba_switch == '1' ? '0' : '1'
            })).json()
            if (res1.result == 'success') {
                createToast('操作成功！', 'green')
            } else {
                createToast('操作失败！', 'red')
            }
            await initSMBStatus()
        } catch (e) {
            // createToast(e.message)
        }
    }
    el.innerHTML = res.samba_switch == '1' ? '关闭SMB' : '开启SMB'
    el.style.backgroundColor = res.samba_switch == '1' ? '#018ad8b0' : ''
}
initSMBStatus()

let initLightStatus = async () => {
    const el = document.querySelector('#LIGHT')
    if (!initRequestData() || !el) {
        el.onclick = () => createToast('请登录', 'red')
        el.style.backgroundColor = '#80808073'
        return null
    }
    let res = await getData(new URLSearchParams({
        cmd: 'indicator_light_switch'
    }))
    if (!el || !res || res.indicator_light_switch == null || res.indicator_light_switch == undefined) return
    el.onclick = async () => {
        if (!initRequestData()) {
            return null
        }
        try {
            const cookie = await login()
            if (!cookie) {
                createToast('登录失败，请检查密码', 'red')
                out()
                return null
            }
            let res1 = await (await postData(cookie, {
                goformId: 'INDICATOR_LIGHT_SETTING',
                indicator_light_switch: res.indicator_light_switch == '1' ? '0' : '1'
            })).json()
            if (res1.result == 'success') {
                createToast('操作成功！', 'green')
            } else {
                createToast('操作失败！', 'red')
            }
            await initLightStatus()
        } catch (e) {
            createToast(e.message, 'red')
        }
    }
    el.innerHTML = res.indicator_light_switch == '1' ? '关闭指示灯' : '开启指示灯'
    el.style.backgroundColor = res.indicator_light_switch == '1' ? '#018ad8b0' : ''
}
initLightStatus()

const initBandForm = async () => {
    const el = document.querySelector('#bandsForm')
    if (!initRequestData() || !el) {
        return null
    }
    let res = await getData(new URLSearchParams({
        cmd: 'lte_band_lock,nr_band_lock'
    }))

    if (!res) return null

    if (res['lte_band_lock']) {
        const bands = res['lte_band_lock'].split(',')
        if (bands && bands.length) {
            for (let band of bands) {
                //  data-type="4G" data-band="5"
                const el = document.querySelector(`#bandsForm input[type="checkbox"][data-band="${band}"][data-type="4G"]`)
                if (el) el.checked = true
            }
        }
    }
    if (res['nr_band_lock']) {
        const bands = res['nr_band_lock'].split(',')
        if (bands && bands.length) {
            for (let band of bands) {
                //  data-type="5G" data-band="5"
                const el = document.querySelector(`#bandsForm input[type="checkbox"][data-band="${band}"][data-type="5G"]`)
                if (el) el.checked = true
            }
        }
    }
}
initBandForm()

const submitBandForm = async (e) => {
    e.preventDefault()
    if (!initRequestData()) {
        out()
        return null
    }
    const form = e.target
    const bands = form.querySelectorAll('input[type="checkbox"]:checked')
    const lte_bands = []
    const nr_bands = []
    //收集选中的数据
    if (bands && bands.length) {
        for (let band of bands) {
            const type = band.getAttribute('data-type')
            const b = band.getAttribute('data-band')
            if (type && b) {
                if (type == '4G') lte_bands.push(b)
                if (type == '5G') nr_bands.push(b)
            }
        }
    }
    const cookie = await login()
    if (!cookie) {
        createToast('登录失败，请检查密码', 'red')
        out()
        return null
    }
    try {
        const res = await (await Promise.all([
            (await postData(cookie, {
                goformId: 'LTE_BAND_LOCK',
                lte_band_lock: lte_bands.join(',')
            })).json(),
            (await postData(cookie, {
                goformId: 'NR_BAND_LOCK',
                nr_band_lock: nr_bands.join(',')
            })).json(),
        ]))
        if (res[0].result == 'success' || res[1].result == 'success') {
            createToast('设置频段成功！', 'green')
        }
        else {
            createToast('设置频段失败', 'red')
        }
    } catch {
        createToast('设置频段失败', 'red')
    } finally {
        await initBandForm()
    }
}

//锁小区
let initCellInfo = async () => {
    try {
        //已锁小区信息
        //小区信息
        const { neighbor_cell_info, locked_cell_info } = await getData(new URLSearchParams({
            cmd: 'neighbor_cell_info,locked_cell_info'
        }))

        if (neighbor_cell_info) {
            const cellBodyEl = document.querySelector('#cellForm tbody')
            cellBodyEl.innerHTML = neighbor_cell_info.map(item => {
                const { band, earfcn, pci, rsrp, rsrq, sinr } = item
                return `
                    <tr onclick="onSelectCellRow(${pci},${earfcn})">
                        <td>${band}</td>
                        <td>${earfcn}</td>
                        <td>${pci}</td>
                        <td>${kano_parseSignalBar(rsrp)}</td>
                        <td>${kano_parseSignalBar(rsrq, -20, -3, -9, -12)}</td>
                        <td>${kano_parseSignalBar(sinr, -10, 30, 13, 0)}</td>
                    </tr>
                `
            }).join('')
        }
        if (locked_cell_info) {
            const lockedCellBodyEl = document.querySelector('#LOCKED_CELL_FORM tbody')
            lockedCellBodyEl.innerHTML = locked_cell_info.map(item => {
                const { earfcn, pci, rat } = item
                return `
                    <tr>
                        <td>${rat == '12' ? '4G' : '5G'}</td>
                        <td>${pci}</td>
                        <td>${earfcn}</td>
                    </tr>
                `
            }).join('')
        }
    } catch (e) {
        // createToast(e.message)
    }
}

let cellInfoRequestTimer = null
initCellInfo()
cellInfoRequestTimer = requestInterval(() => initCellInfo(), 1500)

let onSelectCellRow = (pci, earfcn) => {
    let pci_t = document.querySelector('#PCI')
    let earfcn_t = document.querySelector('#EARFCN')
    if (pci_t && earfcn_t) {
        pci_t.value = pci
        earfcn_t.value = earfcn
        createToast(`已选择: ${pci},${earfcn}`, 'green')
    }
}

//锁小区
const submitCellForm = async (e) => {
    e.preventDefault()
    if (!initRequestData()) {
        out()
        return null
    }
    try {
        const cookie = await login()
        if (!cookie) {
            createToast('登录失败，请检查密码', 'red')
            out()
            return null
        }

        const ratEl = e.target.querySelector('input[name="RAT"]:checked')
        const pciEl = e.target.querySelector('#PCI')
        const earfcnEl = e.target.querySelector('#EARFCN')

        if (!ratEl || !pciEl || !earfcnEl) return

        const form = {
            pci: pciEl.value.trim(),
            earfcn: earfcnEl.value.trim(),
            rat: ratEl.value.trim()
        }

        if (!form.pci || !form.earfcn) {
            createToast('请填写完整数据', 'red')
            return
        }

        const res = await (await postData(cookie, {
            goformId: 'CELL_LOCK',
            ...form
        })).json()

        if (res.result == 'success') {
            pciEl.value = ''
            earfcnEl.value = ''
            createToast('设置小区成功！', 'green')
        } else {
            throw '设置小区失败'
        }
    } catch (e) {
        createToast('设置小区失败', 'red')
    }
}

let unlockAllCell = async () => {
    if (!initRequestData()) {
        out()
        return null
    }
    try {
        const cookie = await login()
        if (!cookie) {
            createToast('登录失败，请检查密码', 'red')
            out()
            return null
        }

        const res = await (await postData(cookie, {
            goformId: 'UNLOCK_ALL_CELL',
        })).json()

        if (res.result == 'success') {
            createToast('解锁小区成功！', 'green')
        } else {
            throw '解锁小区失败'
        }

    } catch {
        createToast('解锁小区失败', 'red')
    }
}

let rebootBtnCount = 1
let rebootTimer = null
let rebootDevice = async (e) => {
    let target = e.target
    if (!initRequestData()) {
        out()
        target.style.backgroundColor = '#80808073'
        return null
    }
    target.style.backgroundColor = ''
    rebootTimer && clearTimeout(rebootTimer)
    if (rebootBtnCount == 1) target.innerHTML = "确定重启？"
    if (rebootBtnCount == 2) target.innerHTML = "那就重启咯？"
    if (rebootBtnCount >= 3) {
        target.innerHTML = "正在重启。。。"
        try {
            const cookie = await login()
            if (!cookie) {
                createToast('登录失败，请检查密码', 'red')
                out()
                return null
            }

            const res = await (await postData(cookie, {
                goformId: 'REBOOT_DEVICE',
            })).json()

            if (res.result == 'success') {
                createToast('重启成功!', 'green')
            } else {
                throw '重启失败！请检查网络'
            }

        } catch {
            createToast('重启失败！请检查网络', 'red')
        }
    }
    rebootBtnCount++
    rebootTimer = setTimeout(() => {
        rebootBtnCount = 1
        target.innerHTML = '重启设备'
    }, 3000);
}

rebootDeviceBtnInit = () => {
    let target = document.querySelector('#REBOOT')
    if (!initRequestData()) {
        target.onclick = () => createToast('请登录', 'red')
        target.style.backgroundColor = '#80808073'
        return null
    }
    target.style.backgroundColor = ''
    target.onclick = rebootDevice
}
rebootDeviceBtnInit()

//字段显示隐藏
document.querySelector("#DICTIONARY").onclick = (e) => {
    showModal('#dictionaryModal', 300, '.8')
}

document.querySelector('#DIC_LIST')?.addEventListener('click', (e) => {
    let target = e.target
    e.stopPropagation()
    e.stopImmediatePropagation()
    if (target.id == 'DIC_LIST') {
        return
    }
    let inputEl = null
    if ((target.tagName).toLowerCase() != 'input') {
        return
    } else {
        inputEl = target
    }
    let id = inputEl.getAttribute('data-name')
    //寻找这个id属于哪个dragList
    const list_id = inputEl.closest("ul").id
    let list_name = null
    if (list_id == "draggable_status") list_name = 'statusShowList'
    if (list_id == "draggable_signal") list_name = 'signalShowList'
    if (list_id == "draggable_props") list_name = 'propsShowList'

    if (list_name == null) return

    let index = showList[list_name].findIndex(i => i.name == id)
    if (index != -1) {
        showList[list_name][index].isShow = inputEl.checked
    }

    localStorage.setItem('showList', JSON.stringify(showList))
}, false)

let resetShowListBtnCount = 1
let resetShowListTimer = null
let resetShowList = (e) => {
    const target = e.target
    resetShowListTimer && clearTimeout(resetShowListTimer)
    if (resetShowListBtnCount == 1) target.innerHTML = "确定？"
    if (resetShowListBtnCount >= 2) {
        localStorage.removeItem('showList');
        localStorage.removeItem('statusShowListDOM');
        localStorage.removeItem('signalShowListDOM');
        localStorage.removeItem('propsShowListDOM');
        location.reload()
    }
    resetShowListBtnCount++
    resetShowListTimer = setTimeout(() => {
        resetShowListBtnCount = 1
        target.innerHTML = '重置(全选)'
    }, 3000);
}


//暂停开始刷新
document.querySelector('#REFRESH').onclick = (e) => {
    if (e.target.innerHTML == '开始刷新') {
        e.target.innerHTML = '停止刷新'
        createToast('已开始刷新', 'green')
        cellInfoRequestTimer = requestInterval(() => initCellInfo(), 1500)
        StopStatusRenderTimer = requestInterval(() => handlerStatusRender(), 800)
    } else {
        e.target.innerHTML = '开始刷新'
        createToast('已停止刷新', 'green')
        StopStatusRenderTimer && StopStatusRenderTimer()
        cellInfoRequestTimer && cellInfoRequestTimer()
    }
}

//流量管理逻辑
document.querySelector("#DataManagement").onclick = async () => {
    if (!initRequestData()) {
        createToast('请登录！', 'red')
        out()
        return null
    }
    // 查流量使用情况
    let res = await getDataUsage()
    if (!res) {
        createToast('获取流量使用情况失败', 'red')
        return null
    }

    res = {
        ...res,
        "wan_auto_clear_flow_data_switch": isNullOrUndefiend(res.wan_auto_clear_flow_data_switch) ? res.wan_auto_clear_flow_data_switch : res.flux_auto_clear_flow_data_switch,
        "data_volume_limit_unit": isNullOrUndefiend(res.data_volume_limit_unit) ? res.data_volume_limit_unit : res.flux_data_volume_limit_unit,
        "data_volume_limit_size": isNullOrUndefiend(res.data_volume_limit_size) ? res.data_volume_limit_size : res.flux_data_volume_limit_size,
        "traffic_clear_date": isNullOrUndefiend(res.traffic_clear_date) ? res.traffic_clear_date : res.flux_clear_date,
        "data_volume_alert_percent": isNullOrUndefiend(res.data_volume_alert_percent) ? res.data_volume_alert_percent : res.flux_data_volume_alert_percent,
        "data_volume_limit_switch": isNullOrUndefiend(res.data_volume_limit_switch) ? res.data_volume_limit_switch : res.flux_data_volume_limit_switch,
    }

    // 预填充表单
    const form = document.querySelector('#DataManagementForm')
    if (!form) return null
    let data_volume_limit_switch = form.querySelector('input[name="data_volume_limit_switch"]')
    let wan_auto_clear_flow_data_switch = form.querySelector('input[name="wan_auto_clear_flow_data_switch"]')
    let data_volume_limit_unit = form.querySelector('input[name="data_volume_limit_unit"]')
    let traffic_clear_date = form.querySelector('input[name="traffic_clear_date"]')
    let data_volume_alert_percent = form.querySelector('input[name="data_volume_alert_percent"]')
    let data_volume_limit_size = form.querySelector('input[name="data_volume_limit_size"]')
    let data_volume_limit_type = form.querySelector('select[name="data_volume_limit_type"]')
    let data_volume_used_size = form.querySelector('input[name="data_volume_used_size"]')
    let data_volume_used_type = form.querySelector('select[name="data_volume_used_type"]')

    // (12094630728720/1024/1024)/1048576
    let used_size_type = 1
    const used_size = (() => {
        const total_bytes = ((Number(res.monthly_rx_bytes) + Number(res.monthly_tx_bytes))) / Math.pow(1024, 2)

        if (total_bytes < 1024) {
            return total_bytes.toFixed(2)
        } else if (total_bytes >= 1024 && total_bytes < Math.pow(1024, 2)) {
            used_size_type = 1024
            return (total_bytes / 1024).toFixed(2)
        } else {
            used_size_type = Math.pow(1024, 2)
            return (total_bytes / Math.pow(1024, 2)).toFixed(2)
        }
    })()

    data_volume_limit_switch && (data_volume_limit_switch.checked = res.data_volume_limit_switch.toString() == '1')
    wan_auto_clear_flow_data_switch && (wan_auto_clear_flow_data_switch.checked = res.wan_auto_clear_flow_data_switch.toString() == 'on')
    data_volume_limit_unit && (data_volume_limit_unit.checked = res.data_volume_limit_unit.toString() == 'data')
    traffic_clear_date && (traffic_clear_date.value = res.traffic_clear_date.toString())
    data_volume_alert_percent && (data_volume_alert_percent.value = res.data_volume_alert_percent.toString())
    data_volume_limit_size && (data_volume_limit_size.value = res.data_volume_limit_size?.split('_')[0].toString())
    data_volume_limit_type && (() => {
        const val = Number(res.data_volume_limit_size?.split('_')[1])
        const option = data_volume_limit_type.querySelector(`option[data-value="${val}"]`)
        option && (option.selected = true)
    })()
    data_volume_used_size && (data_volume_used_size.value = used_size.toString())
    data_volume_used_type && (() => {
        const option = data_volume_used_type.querySelector(`option[data-value="${used_size_type.toFixed(0)}"]`)
        option && (option.selected = true)
    })()
    showModal('#DataManagementModal')
}

//流量管理表单提交
let handleDataManagementFormSubmit = async (e) => {
    e.preventDefault();
    try {
        const cookie = await login()
        if (!cookie) {
            createToast('登录失败，请检查密码', 'red')
            closeModal('#DataManagementModal')
            setTimeout(() => {
                out()
            }, 310);
            return null
        }

        let form_data = {
            "data_volume_limit_switch": "0",
            "wan_auto_clear_flow_data_switch": "off",
            "data_volume_limit_unit": "data",
            "traffic_clear_date": "0",
            "data_volume_alert_percent": "0",
            "data_volume_limit_size": "0",
            "data_volume_limit_type": "1", //MB GB TB
            "data_volume_used_size": "0",
            "data_volume_used_type": "1", //MB GB TB
            // 时间
            "notify_deviceui_enable": "0",
        }

        const form = e.target; // 获取表单
        const formData = new FormData(form);

        for (const [key, value] of formData.entries()) {
            switch (key) {
                case 'data_volume_limit_switch':
                    form_data[key] = value.trim() == 'on' ? '1' : '0'
                    form_data['flux_data_volume_limit_switch'] = value.trim() == 'on' ? '1' : '0'
                    break;
                case 'wan_auto_clear_flow_data_switch':
                    form_data[key] = value.trim() == 'on' ? 'on' : '0'
                    form_data['flux_auto_clear_flow_data_switch'] = value.trim() == 'on' ? 'on' : '0'
                    break;
                case 'data_volume_limit_unit':
                    form_data[key] = value.trim() == 'on' ? 'data' : 'time'
                    form_data['flux_data_volume_limit_unit'] = value.trim() == 'on' ? 'data' : 'time'
                    break;
                case 'traffic_clear_date':
                    if (isNaN(Number(value.trim()))) {
                        createToast('清零日期必须为数字', 'red')
                        return
                    }
                    if (Number(value.trim()) < 0 || Number(value.trim()) > 31) {
                        createToast('清零日期必须在0-31之间', 'red')
                        return
                    }
                    form_data[key] = value.trim()
                    form_data['flux_clear_date'] = value.trim()
                    break;
                case 'data_volume_alert_percent':
                    if (isNaN(Number(value.trim()))) {
                        createToast('提醒阈值必须为数字', 'red')
                        return
                    }
                    if (Number(value.trim()) < 0 || Number(value.trim()) > 100) {
                        createToast('提醒阈值必须在0-100之间', 'red')
                        return
                    }
                    form_data[key] = value.trim()
                    form_data['flux_data_volume_alert_percent'] = value.trim()
                    break;
                case 'data_volume_limit_size':
                    if (isNaN(Number(value.trim()))) {
                        createToast('流量套餐必须为数字', 'red')
                        return
                    }
                    if (Number(value.trim()) <= 0) {
                        createToast('流量套餐必须大于0', 'red')
                        return
                    }
                    form_data[key] = value.trim()
                    form_data['flux_data_volume_limit_size'] = value.trim()
                    break;
                case 'data_volume_limit_type':
                    form_data[key] = '_' + value.trim()
                    form_data['flux_data_volume_limit_type'] = '_' + value.trim()
                    break;
                case 'data_volume_used_size':
                    if (isNaN(Number(value.trim()))) {
                        createToast('已用流量必须为数字', 'red')
                        return
                    }
                    if (Number(value.trim()) <= 0) {
                        createToast('已用流量必须大于0', 'red')
                        return
                    }
                    form_data[key] = value.trim()
                    break;
                case 'data_volume_used_type':
                    form_data[key] = value.trim()
                    break;
            }
        }
        form_data['data_volume_limit_size'] = form_data['data_volume_limit_size'] + form_data['data_volume_limit_type']
        form_data['flux_data_volume_limit_size'] = form_data['data_volume_limit_size']
        const used_data = Number(form_data.data_volume_used_size) * Number(form_data['data_volume_used_type']) * Math.pow(1024, 2)
        const clear_form_data = {
            data_volume_limit_switch: form_data['data_volume_limit_switch'],
            wan_auto_clear_flow_data_switch: 'on',
            traffic_clear_date: '1',
            notify_deviceui_enable: '0',
            flux_data_volume_limit_switch: form_data['data_volume_limit_switch'],
            flux_auto_clear_flow_data_switch: 'on',
            flux_clear_date: '1',
            flux_notify_deviceui_enable: '0'
        }
        delete form_data['data_volume_limit_type']
        //发请求
        try {
            const tempData = form_data['data_volume_limit_switch'] == '0' ? clear_form_data : form_data
            const res = await (await postData(cookie, {
                goformId: 'DATA_LIMIT_SETTING',
                ...tempData
            })).json()

            const res1 = await (await postData(cookie, {
                goformId: 'FLOW_CALIBRATION_MANUAL',
                calibration_way: form_data.data_volume_limit_unit,
                time: 0,
                data: used_data.toFixed(0)
            })).json()

            if (res.result == 'success' && res1.result == 'success') {
                createToast('设置成功!', 'green')
                closeModal('#DataManagementModal')
            } else {
                throw '设置失败！请检查网络'
            }
        } catch (e) {
            createToast(e.message, 'red')
        }
    } catch (e) {
        createToast(e.message, 'red')
    }
};


//WIFI管理逻辑
let initWIFIManagementForm = async () => {
    try {
        let { WiFiModuleSwitch, ResponseList } = await getData(new URLSearchParams({
            cmd: 'queryWiFiModuleSwitch,queryAccessPointInfo'
        }))

        const WIFIManagementForm = document.querySelector('#WIFIManagementForm')
        if (!WIFIManagementForm) return

        if (WiFiModuleSwitch == "1" && ResponseList?.length) {
            for (let index in ResponseList) {
                if (ResponseList[index].AccessPointSwitchStatus == '1') {
                    let item = ResponseList[index]
                    let apEl = WIFIManagementForm.querySelector('input[name="AccessPointIndex"]')
                    let chipEl = WIFIManagementForm.querySelector('input[name="ChipIndex"]')
                    let ApMaxStationNumberEl = WIFIManagementForm.querySelector('input[name="ApMaxStationNumber"]')
                    let PasswordEl = WIFIManagementForm.querySelector('input[name="Password"]')
                    let ApBroadcastDisabledEl = WIFIManagementForm.querySelector('input[name="ApBroadcastDisabled"]')
                    let SSIDEl = WIFIManagementForm.querySelector('input[name="SSID"]')
                    let QRCodeImg = document.querySelector("#QRCodeImg")
                    let AuthModeEl = WIFIManagementForm.querySelector('select[name="AuthMode"]')
                    apEl && (apEl.value = item.AccessPointIndex)
                    chipEl && (chipEl.value = item.ChipIndex)
                    ApMaxStationNumberEl && (ApMaxStationNumberEl.value = item.ApMaxStationNumber)
                    PasswordEl && (PasswordEl.value = decodeBase64(item.Password))
                    ApBroadcastDisabledEl && (ApBroadcastDisabledEl.checked = item.ApBroadcastDisabled.toString() == '0')
                    SSIDEl && (SSIDEl.value = item.SSID)
                    // 二维码
                    QRCodeImg.src = KANO_baseURL + item.QrImageUrl
                    const WIFI_FORM_SHOWABLE = document.querySelector('#WIFI_FORM_SHOWABLE')
                    AuthModeEl.value = item.AuthMode
                    AuthModeEl.selected = item.AuthMode
                    if (AuthModeEl && WIFI_FORM_SHOWABLE) {
                        const option = AuthModeEl.querySelector(`option[data-value="${item.AuthMode}"]`)
                        option && (option.selected = "selected")
                        if (item.AuthMode == "OPEN") {
                            WIFI_FORM_SHOWABLE.style.display = 'none'
                        } else {
                            WIFI_FORM_SHOWABLE.style.display = ''
                        }
                    }

                }
            }
        }
    }
    catch (e) {
        console.error(e.message)
        // createToast(e.message)
    }
}

document.querySelector("#WIFIManagement").onclick = async () => {
    if (!initRequestData()) {
        createToast('请登录！', 'red')
        out()
        return null
    }
    await initWIFIManagementForm()
    showModal("#WIFIManagementModal")
}

let handleWIFIManagementFormSubmit = async (e) => {
    e.preventDefault();
    try {
        const cookie = await login()
        if (!cookie) {
            createToast('登录失败，请检查密码', 'red')
            closeModal('#WIFIManagementModal')
            setTimeout(() => {
                out()
            }, 310);
            return null
        }

        const form = e.target; // 获取表单
        const formData = new FormData(form);

        let data = {
            SSID: '',
            AuthMode: '',
            EncrypType: '',
            Password: '',
            ApMaxStationNumber: '',
            ApBroadcastDisabled: 1,
            ApIsolate: 0,
            ChipIndex: 0,
            AccessPointIndex: 0
        }

        for (const [key, value] of formData.entries()) {
            switch (key) {
                case 'SSID':
                    value.trim() && (data[key] = value.trim())
                    break;
                case 'AuthMode':
                    value == 'OPEN' ? data['EncrypType'] = "NONE" : data['EncrypType'] = "CCMP"
                    value.trim() && (data[key] = value.trim())
                    break;
                case 'ApBroadcastDisabled':
                    data[key] = value == 'on' ? 0 : 1
                    break;
                case 'Password':
                    // if(!value.trim()) createToast('请输入密码！')
                    value.trim() && (data[key] = encodeBase64(value.trim()))
                    break;
                case 'ApIsolate':
                case 'ApMaxStationNumber':
                case 'AccessPointIndex':
                case 'ChipIndex':
                    !isNaN(Number(value.trim())) && (data[key] = Number(value.trim()))
                    break;
            }
        }

        if (data.AuthMode == 'OPEN' || data.EncrypType == "NONE") {
            delete data.Password
        } else {
            if (data.Password.length == 0) {
                return createToast('请输入密码', 'red')
            }
            if (data.Password.length < 8) {
                return createToast('密码至少8位数', 'red')
            }
            if (data.ApMaxStationNumber.length <= 0) {
                return createToast('最大接入必须大于0', 'red')
            }
        }

        const res = await (await postData(cookie, {
            goformId: 'setAccessPointInfo',
            ...data
        })).json()

        if (res.result == 'success') {
            createToast('设置成功! 请重新连接WIFI！', 'green')
            closeModal('#WIFIManagementModal')
        } else {
            throw '设置失败！请检查网络'
        }
    }
    catch (e) {
        console.error(e.message)
        // createToast(e.message)
    }

}

let handleWifiEncodeChange = (event) => {
    const WIFI_FORM_SHOWABLE = document.querySelector('#WIFI_FORM_SHOWABLE')
    const target = event.target
    if (target) {
        console.log(target.value);
        if (WIFI_FORM_SHOWABLE) {
            if (target.value == "OPEN") {
                WIFI_FORM_SHOWABLE.style.display = 'none'
            } else {
                WIFI_FORM_SHOWABLE.style.display = ''
            }
        }
    }
}

let handleShowPassword = (e) => {
    const target = e.target
    const WIFI_PASSWORD = document.querySelector('#WIFI_PASSWORD')
    if (target && WIFI_PASSWORD) {
        WIFI_PASSWORD.setAttribute('type', target.checked ? "text" : "password")
    }
}

document.querySelector('#tokenInput').addEventListener('keydown', (event) => {
    if (event.key === 'Enter') {
        onTokenConfirm()
    }
});

//无线设备管理
document.querySelector('#ClientManagement').onclick = async () => {
    if (!initRequestData()) {
        createToast('请登录！', 'red')
        out()
        return null
    }
    await initClientManagementModal()
    showModal('#ClientManagementModal')
}

let initClientManagementModal = async () => {
    try {
        // 获取连接设备
        const { station_list, lan_station_list, BlackMacList, BlackNameList, AclMode } = await getData(new URLSearchParams({
            cmd: 'station_list,lan_station_list,queryDeviceAccessControlList'
        }))
        const blackMacList = BlackMacList ? BlackMacList.split(';') : []
        const blackNameList = BlackNameList ? BlackNameList.split(';') : []

        const CONN_CLIENT_LIST = document.querySelector('#CONN_CLIENT_LIST')
        const BLACK_CLIENT_LIST = document.querySelector('#BLACK_CLIENT_LIST')

        //渲染设备列表
        let conn_client_html = ''
        let black_list_html = ''

        if (station_list && station_list.length) {
            conn_client_html += station_list.map(({ hostname, ip_addr, mac_addr }) => (`
            <div style="display: flex;width: 100%;margin: 10px 0;overflow: auto;"
                class="card-item">
                <div style="margin-right: 10px;">
                    <p style="display: flex;justify-content: space-between;">
                        <span style="justify-self: start;">主机名称：</span>
                        <span onclick="copyText(event)">${hostname}</span>
                    </p>
                    <p style="display: flex;justify-content: space-between;">
                        <span style="justify-self: start;">MAC地址：</span>
                        <span onclick="copyText(event)">${mac_addr}</span>
                    </p>
                    <p style="display: flex;justify-content: space-between;">
                        <span style="justify-self: start;">IP地址：</span>
                        <span onclick="copyText(event)">${ip_addr}</span>
                    </p>
                    <p style="display: flex;justify-content: space-between;">
                        <span style="justify-self: start;">接入类型：</span>
                        <span>无线</span>
                    </p>
                </div>
                <div style="flex:1;text-align: right;">
                    <button class="btn" style="padding: 20px 4px;" onclick="setOrRemoveDeviceFromBlackList('${[mac_addr, ...blackMacList].join(';')}','${[hostname, ...blackNameList].join(';')}','${AclMode}')">🚫 拉黑</button>
                </div>
            </div>`)).join('')
        }
        if (lan_station_list && lan_station_list.length) {
            conn_client_html += lan_station_list.map(({ hostname, ip_addr, mac_addr }) => (`
            <div style="display: flex;width: 100%;margin: 10px 0;overflow: auto;"
                class="card-item">
                <div style="margin-right: 10px;">
                    <p style="display: flex;justify-content: space-between;">
                        <span style="justify-self: start;">主机名称：</span>
                        <span onclick="copyText(event)">${hostname}</span>
                    </p>
                    <p style="display: flex;justify-content: space-between;">
                        <span style="justify-self: start;">MAC地址：</span>
                        <span onclick="copyText(event)">${mac_addr}</span>
                    </p>
                    <p style="display: flex;justify-content: space-between;">
                        <span style="justify-self: start;">IP地址：</span>
                        <span onclick="copyText(event)">${ip_addr}</span>
                    </p>
                    <p style="display: flex;justify-content: space-between;">
                        <span style="justify-self: start;">接入类型：</span>
                        <span>有线</span>
                    </p>
                </div>
                <div style="flex:1;text-align: right;">
                    <button class="btn" style="padding: 20px 4px;" onclick="setOrRemoveDeviceFromBlackList('${[mac_addr, ...blackMacList].join(';')}','${[hostname, ...blackNameList].join(';')}','${AclMode}')">🚫 拉黑</button>
                </div>
            </div>`)).join('')
        }
        if (blackMacList.length && blackNameList.length) {
            black_list_html += blackMacList.map((item, index) => {
                if (item) {
                    let params = `'${blackMacList.filter(i => item != i).join(';')}'` + ","
                        + `'${blackMacList.filter(i => blackNameList[index] != i).join(';')}'` + ","
                        + `'${AclMode}'`
                    return `
                    <div style="display: flex;width: 100%;margin: 10px 0;overflow: auto;"
                        class="card-item">
                        <div style="margin-right: 10px;">
                            <p style="display: flex;justify-content: space-between;">
                                <span style="justify-self: start;">主机名称：</span>
                                <span onclick="copyText(event)">${blackNameList[index] ? blackNameList[index] : '未知'}</span>
                            </p>
                            <p style="display: flex;justify-content: space-between;">
                                <span style="justify-self: start;">MAC地址：</span>
                                <span onclick="copyText(event)">${item}</span>
                            </p>
                        </div>
                        <div style="flex:1;text-align: right;">
                            <button class="btn" style="padding: 20px 4px;" onclick="setOrRemoveDeviceFromBlackList(${params})">✅ 解封</button>
                        </div>
                    </div>`
                }
            }).join('')
        }

        if (conn_client_html == '') conn_client_html = '<p>暂无设备</p>'
        if (black_list_html == '') black_list_html = '<p>暂无设备</p>'
        CONN_CLIENT_LIST && (CONN_CLIENT_LIST.innerHTML = conn_client_html)
        BLACK_CLIENT_LIST && (BLACK_CLIENT_LIST.innerHTML = black_list_html)
    } catch (e) {
        console.error(e);
        createToast('获取数据失败，请检查网络连接', 'red')
    }
}

let setOrRemoveDeviceFromBlackList = async (BlackMacList, BlackNameList, AclMode) => {
    try {
        const cookie = await login()
        if (!cookie) {
            createToast('登录失败，请检查密码', 'red')
            closeModal('#ClientManagementModal')
            setTimeout(() => {
                out()
            }, 310);
            return null
        }
        const res = await postData(cookie, {
            goformId: "setDeviceAccessControlList",
            AclMode: AclMode.trim(),
            WhiteMacList: "",
            BlackMacList: BlackMacList.trim(),
            WhiteNameList: "",
            BlackNameList: BlackNameList.trim()
        })
        const { result } = await res.json()
        if (result && result == 'success') {
            createToast('设置成功', 'green')
        } else {
            createToast('设置失败', 'red')
        }
        await initClientManagementModal()
    }
    catch (e) {
        console.error(e);
        createToast('请求数据失败，请检查网络连接', 'red')
    }
}

let closeClientManager = () => {
    closeModal('#ClientManagementModal')
}

//开关蜂窝数据
let handlerCecullarStatus = async () => {
    const btn = document.querySelector('#CECULLAR')
    if (!initRequestData()) {
        btn.onclick = () => createToast('请登录', 'red')
        btn.style.backgroundColor = '#80808073'
        return null
    }
    let res = await getData(new URLSearchParams({
        cmd: 'ppp_status'
    }))
    btn.onclick = async () => {
        try {
            if (!initRequestData()) {
                return null
            }
            const cookie = await login()
            if (!cookie) {
                createToast('登录失败，请检查密码', 'red')
                out()
                return null
            }
            btn.innerHTML = '正在更改...'
            let res1 = await (await postData(cookie, {
                goformId: res.ppp_status == 'ppp_disconnected' ? 'CONNECT_NETWORK' : 'DISCONNECT_NETWORK',
            })).json()
            if (res1.result == 'success') {
                setTimeout(async () => {
                    await handlerCecullarStatus()
                    createToast('操作成功！', 'green')
                }, 2000);
            } else {
                createToast('操作失败！', 'red')
            }
        } catch (e) {
            // createToast(e.message)
        }
    }
    btn.innerHTML = res.ppp_status == 'ppp_disconnected' ? '开启蜂窝数据' : '关闭蜂窝数据'
    btn.style.backgroundColor = res.ppp_status == 'ppp_disconnected' ? '' : '#018ad8b0'
}
handlerCecullarStatus()

// title
const loadTitle = async () => {
    try {
        const { app_ver } = await (await fetch(`${KANO_baseURL}/battery_and_model`)).json()
        document.querySelector('#TITLE').innerHTML = "ZTE-UFI-TOOLS-WEB Ver: " + app_ver
        document.querySelector('#MAIN_TITLE').innerHTML = "ZTE-UFI管理工具 Ver: " + app_ver
    } catch {/*没有，不处理*/ }
}
loadTitle()

//设置背景图片
document.querySelector('#BG_SETTING').onclick = () => {
    showModal('#bgSettingModal')
}

let handleSubmitBg = () => {
    const imgUrl = document.querySelector('#BG_INPUT')?.value
    const bg_checked = document.querySelector('#isCheckedBG')?.checked
    const BG = document.querySelector('#BG')
    const BG_OVERLAY = document.querySelector('#BG_OVERLAY')
    if (!imgUrl == undefined || !BG || bg_checked == undefined || !BG_OVERLAY) return
    if (!bg_checked) {
        BG.style.backgroundImage = 'unset'
        BG_OVERLAY.style.background = 'transparent'
        localStorage.removeItem('backgroundUrl')
    } else {
        BG.style.backgroundImage = `url(${imgUrl})`
        BG_OVERLAY.style.background = 'var(--dark-bgi-color)'
        // 保存
        localStorage.setItem('backgroundUrl', imgUrl)
    }
    createToast('保存成功~', 'green')
    closeModal('#bgSettingModal')
}

//初始化背景图片
(() => {
    const BG = document.querySelector('#BG')
    const imgUrl = localStorage.getItem('backgroundUrl')
    const isCheckedBG = document.querySelector('#isCheckedBG')
    const BG_INPUT = document.querySelector('#BG_INPUT')
    if (!BG || !isCheckedBG || !BG_INPUT) return
    isCheckedBG.checked = imgUrl ? true : false
    BG_INPUT.value = imgUrl
    if (!imgUrl) {
        const BG_OVERLAY = document.querySelector('#BG_OVERLAY')
        BG_OVERLAY && (BG_OVERLAY.style.background = 'transparent')
        return
    }
    BG.style.backgroundImage = `url(${imgUrl})`
})()

//定时重启模态框
let initScheduleRebootStatus = async () => {
    const btn = document.querySelector('#SCHEDULE_REBOOT')
    const SCHEDULE_TIME = document.querySelector('#SCHEDULE_TIME')
    const SCHEDULE_ENABLED = document.querySelector('#SCHEDULE_ENABLED')
    if (!btn) return
    if (!initRequestData()) {
        btn.onclick = () => createToast('请登录', 'red')
        btn.style.backgroundColor = '#80808073'
        return null
    }

    const { restart_schedule_switch, restart_time } = await getData(new URLSearchParams({
        cmd: 'restart_schedule_switch,restart_time'
    }))

    SCHEDULE_ENABLED.checked = restart_schedule_switch == '1'
    SCHEDULE_TIME.value = restart_time
    btn.style.backgroundColor = restart_schedule_switch == '1' ? '#018ad8b0' : ''

    btn.onclick = () => {
        if (!initRequestData()) {
            btn.onclick = () => createToast('请登录', 'red')
            btn.style.backgroundColor = '#80808073'
            return null
        }
        showModal('#scheduleRebootModal')
    }
}
initScheduleRebootStatus()

let handleScheduleRebootFormSubmit = async (e) => {
    e.preventDefault()
    const data = {
        restart_schedule_switch: "0",
        restart_time: '00:00'
    }
    const form = e.target; // 获取表单
    const formData = new FormData(form);
    let regx = /^(0?[0-9]|1[0-9]|2[0-3]):(0?[0-9]|[1-5][0-9])$/
    for ([key, value] of formData.entries()) {
        switch (key) {
            case 'restart_time':
                if (!regx.exec(value.trim()) || !value.trim()) return createToast('请输入正确的重启时间 (00:00-23:59)', 'red')
                data.restart_time = value.trim()
                break;
            case 'restart_schedule_switch':
                data.restart_schedule_switch = value == 'on' ? '1' : '0'
        }
    }
    try {
        const cookie = await login()
        try {
            const res = await (await postData(cookie, {
                goformId: 'RESTART_SCHEDULE_SETTING',
                restart_time: data.restart_time,
                restart_schedule_switch: data.restart_schedule_switch
            })).json()
            if (res?.result == 'success') {
                createToast('设置成功！', 'green')
                closeModal('#scheduleRebootModal')
            } else {
                throw '设置失败'
            }
        } catch {
            createToast('设置失败！', 'red')
        }
    } catch {
        createToast('登录失败，请检查密码和网络连接', 'red')
    }
}

// U30AIR用关机指令
let initShutdownBtn = async () => {
    const btn = document.querySelector('#SHUTDOWN')
    if (!btn) return
    if (!initRequestData()) {
        btn.onclick = () => createToast('请登录', 'red')
        btn.style.backgroundColor = '#80808073'
        return null
    }

    const { battery_value, battery_vol_percent } = await getData(new URLSearchParams({
        cmd: 'battery_value,battery_vol_percent'
    }))

    if (battery_value && battery_vol_percent && (battery_value != '' && battery_vol_percent != '')) {
        // 显示按钮
        btn.style.display = ''

    } else {
        //没电池的不显示此按钮
        btn.style.display = 'none'
    }
    btn.style.backgroundColor = '#018ad8b0'
    btn.onclick = async () => {
        if (!initRequestData()) {
            btn.onclick = () => createToast('请登录', 'red')
            btn.style.backgroundColor = '#80808073'
            return null
        }
        try {
            const cookie = await login()
            try {
                const res = await (await postData(cookie, {
                    goformId: 'SHUTDOWN_DEVICE'
                })).json()

                if (res?.result == 'success') {
                    createToast('关机成功！', 'green')
                } else {
                    createToast('关机失败', 'red')
                }
            } catch {
                createToast('关机失败', 'red')
            }
        } catch {
            createToast('登录失败，请检查密码和网络连接', 'red')
        }
    }
}
initShutdownBtn()

// 启用TTYD（如果有）
let initTTYD = async () => {
    const TTYD = document.querySelector('#TTYD')
    if (!TTYD) return
    const list = TTYD.querySelector('.deviceList')
    if (!list) return
    //fetch TTYD地址，如有，则显示
    try {
        const port = localStorage.getItem('ttyd_port')
        if (!port) return
        const TTYD_INPUT = document.querySelector('#TTYD_INPUT')
        TTYD_INPUT && (TTYD_INPUT.value = port)
        const res = await (await fetch(`${KANO_baseURL}/hasTTYD?port=${port}`, {
            method: "get",
        })).json()
        if (res.code !== '200') {
            TTYD.style.display = 'none'
            list.innerHTML = ``
            return
        }
        console.log('TTYD已找到，正在启用。。。')
        TTYD.style.display = ''
        list.innerHTML = `
        <li style = "padding:10px">
                    <iframe src="http://${res.ip}" style="border:none;padding:0;margin:0;width:100%;height:400px;border-radius: 10px;overflow: hidden;opacity: .6;"></iframe>
        </li > `
    } catch {
        // console.log();
    }
}
initTTYD()

let click_count_ttyd = 1
let ttyd_timer = null
let enableTTYD = () => {
    click_count_ttyd++
    if (click_count_ttyd == 4) {
        // 启用ttyd弹窗
        showModal('#TTYDModal')
    }
    ttyd_timer && clearInterval(ttyd_timer)
    ttyd_timer = setTimeout(() => {
        click_count_ttyd = 1
    }, 1999)
}

let handleTTYDFormSubmit = (e) => {
    e.preventDefault()
    const form = e.target
    const formData = new FormData(form);
    const ttyd_port = formData.get('ttyd_port')
    if (!ttyd_port || ttyd_port.trim() == '') return createToast('请填写端口', 'red')
    let ttydNumber = Number(ttyd_port.trim())
    if (isNaN(ttydNumber) || ttydNumber <= 0 || ttydNumber > 65535) return createToast('请填写正确的端口', 'red')
    // 保存ttyd port
    localStorage.setItem('ttyd_port', ttyd_port)
    createToast('保存成功', 'green')
    closeModal('#TTYDModal')
    initTTYD()
}

const executeATCommand = async (command) => {
    try {
        const command_enc = encodeURIComponent(command)
        const res = await (await fetch(`${KANO_baseURL}/AT?command=${command_enc}`)).json()
        return res
    } catch (e) {
        return null
    }
}

let initATBtn = () => {
    const el = document.querySelector('#AT')
    if (!initRequestData() || !el) {
        el.onclick = () => createToast('请登录', 'red')
        el.style.backgroundColor = '#80808073'
        return null
    }
    el.style.backgroundColor = ''
    el.onclick = () => {
        showModal('#ATModal')
    }
}
initATBtn()

const handleATFormSubmit = async () => {
    const AT_value = document.querySelector('#AT_INPUT')?.value;
    if (!AT_value || AT_value.trim() === '') {
        return createToast('请输入AT指令', 'red');
    }

    const AT_RESULT = document.querySelector('#AT_RESULT');
    AT_RESULT.innerHTML = "执行中,请耐心等待...";

    try {
        const res = await executeATCommand(AT_value.trim());

        if (res) {
            if (res.error) {
                AT_RESULT.innerHTML = res.error;
                createToast('执行失败', 'red');
                return;
            }

            AT_RESULT.innerHTML = res.result;
            createToast('执行成功', 'green');
        } else {
            createToast('执行失败', 'red');
        }

    } catch (err) {
        const error = err?.error || '未知错误';
        AT_RESULT.innerHTML = error;
        createToast('执行失败', 'red');
    }
};

const handleQosAT = async () => {
    const AT_RESULT = document.querySelector('#AT_RESULT');
    AT_RESULT.innerHTML = "执行中,请耐心等待...";

    try {
        const res = await executeATCommand('AT+CGEQOSRDP=1');

        if (res) {
            if (res.error) {
                AT_RESULT.innerHTML = res.error;
                createToast('执行失败', 'red');
                return;
            }

            AT_RESULT.innerHTML = res.result;
            createToast('执行成功', 'green');
        } else {
            createToast('执行失败', 'red');
        }

    } catch (err) {
        const error = err?.error || '未知错误';
        AT_RESULT.innerHTML = error;
        createToast('执行失败', 'red');
    }
};

const handleQosIMEI = async () => {
    // 执行AT
    const AT_RESULT = document.querySelector('#AT_RESULT')
    AT_RESULT.innerHTML = "执行中,请耐心等待..."
    try {
        const res = await executeATCommand('AT+CGSN');
        if (res) {
            if (res.error) {
                AT_RESULT.innerHTML = res.error;
                createToast('执行失败', 'red');
                return;
            }

            AT_RESULT.innerHTML = res.result;
            createToast('执行成功', 'green');
        } else {
            createToast('执行失败', 'red');
        }
    } catch (err) {
        const error = err?.error || '未知错误';
        AT_RESULT.innerHTML = error;
        createToast('执行失败', 'red');
    }
}


//展开收起
const CLPS = document.querySelector('#CLPS')
const boxEl = document.querySelector('.collapse')
window.addEventListener('resize', () => {
    const box = boxEl.querySelector('.collapse_box')
    const value = boxEl.getAttribute('data-name');
    if (!box || value != 'open') return
    boxEl.style.height = box.getBoundingClientRect().height + 'px'
})
const observer = new MutationObserver((mutationsList) => {
    for (const mutation of mutationsList) {
        if (
            mutation.type === 'attributes' &&
            mutation.attributeName === 'data-name'
        ) {
            const newValue = boxEl.getAttribute('data-name');
            const box = boxEl.querySelector('.collapse_box')
            if (!box) return

            if (newValue == 'open') {
                boxEl.style.height = box.getBoundingClientRect().height + 'px'
                boxEl.style.overflow = 'hidden'
            } else {
                boxEl.style.height = '0'
                boxEl.style.overflow = 'hidden'
            }
        }
    }
});

// 配置观察器
observer.observe(boxEl, {
    attributes: true, // 监听属性变化
    attributeFilter: ['data-name'], // 只监听 data-name 属性
});

CLPS.onclick = () => {
    if (boxEl && boxEl.dataset) {
        boxEl.dataset.name = boxEl.dataset.name == 'open' ? 'close' : 'open'
        localStorage.setItem('collapse', boxEl.dataset.name)
    }
}
boxEl.dataset.name = localStorage.getItem('collapse') || 'open'


//执行时禁用按钮
const disableButtonWhenExecuteFunc = async (e, func) => {
    const target = e.currentTarget

    target.setAttribute("disabled", "true");
    target.style.opacity = '.5'
    try {
        func && await func()
    } finally {
        target.removeAttribute("disabled");
        target.style.opacity = ''
    }
}

//执行smb目录更改
const handleSambaPath = async (command = '/') => {
    const AT_RESULT = document.querySelector('#AT_RESULT')
    AT_RESULT.innerHTML = "执行中,请耐心等待..."
    try {
        const command_enc = encodeURIComponent(command)
        const res = await (await fetch(`${KANO_baseURL}/smbPath?path=${command_enc}`)).json()
        if (res) {
            if (res.error) {
                AT_RESULT.innerHTML = res.error;
                createToast('执行失败', 'red');
                return;
            }
            AT_RESULT.innerHTML = res.result;
            createToast('执行完成', 'green');
        } else {
            AT_RESULT.innerHTML = '';
            createToast('执行失败', 'red');
        }
    } catch (e) {
        AT_RESULT.innerHTML = '';
        createToast('执行失败', 'red');
    }
}

//更改密码
initChangePassData = () => {
    const el = document.querySelector("#CHANGEPWD")
    if (!initRequestData() || !el) {
        el.onclick = () => createToast('请登录', 'red')
        el.style.backgroundColor = '#80808073'
        return null
    }
    el.style.backgroundColor = '#87ceeb70'
    el.onclick = async () => {
        showModal('#changePassModal')
    }
}
initChangePassData()

const handleChangePassword = async (e) => {
    e.preventDefault()
    const form = e.target
    const formData = new FormData(form);
    const oldPassword = formData.get('oldPassword')
    const newPassword = formData.get('newPassword')
    const confirmPassword = formData.get('confirmPassword')
    if (!oldPassword || oldPassword.trim() == '') return createToast('请输入旧密码', 'red')
    if (!newPassword || newPassword.trim() == '') return createToast('请输入新密码', 'red')
    if (!confirmPassword || confirmPassword.trim() == '') return createToast('请确认新密码', 'red')
    if (newPassword != confirmPassword) return createToast('两次输入的新密码不一致', 'red')

    try {
        const cookie = await login()
        try {
            const res = await (await postData(cookie, {
                goformId: 'CHANGE_PASSWORD',
                oldPassword: SHA256(oldPassword),
                newPassword: SHA256(newPassword)
            })).json()
            if (res?.result == 'success') {
                createToast('修改成功！', 'green')
                form.reset()
                closeModal('#changePassModal')
            } else {
                throw '修改失败'
            }
        } catch {
            createToast('修改失败！', 'red')
        }
    } catch {
        createToast('登录失败，请检查密码和网络连接', 'red')
        closeModal('#changePassModal')
        setTimeout(() => {
            out()
        }, 310);
    }
}

const onCloseChangePassForm = () => {
    const form = document.querySelector("#changePassForm")
    form && form.reset()
    closeModal("#changePassModal")
}

//sim卡切换
let initSimCardType = async () => {
    const selectEl = document.querySelector('#SIM_CARD_TYPE')
    //查询是否支持双卡
    const { dual_sim_support } = await getData(new URLSearchParams({
        cmd: 'dual_sim_support'
    }))
    if (dual_sim_support && dual_sim_support == '0') {
        return
    } else {
        selectEl.style.display = ''
    }
    if (!initRequestData() || !selectEl) {
        selectEl.style.backgroundColor = '#80808073'
        selectEl.disabled = true
        return null
    }
    selectEl.style.backgroundColor = ''
    selectEl.disabled = false
    let res = await getData(new URLSearchParams({
        cmd: 'sim_slot'
    }))
    if (!selectEl || !res || res.sim_slot == null || res.sim_slot == undefined) {
        return
    }
    [...selectEl.children].forEach((item) => {
        if (item.value == res.sim_slot) {
            item.selected = true
        }
    })
}
initSimCardType()

let changeSimCard = async (e) => {
    const value = e.target.value.trim()
    if (!initRequestData() || !value) {
        return null
    }
    createToast('更改中，请稍后', '#BF723F')
    try {
        const cookie = await login()
        if (!cookie) {
            createToast('登录失败，请检查密码', 'red')
            out()
            return null
        }
        let res = await (await postData(cookie, {
            goformId: 'SET_SIM_SLOT',
            sim_slot: value.trim()
        })).json()
        if (res.result == 'success') {
            createToast('操作成功！', 'green')
        } else {
            createToast('操作失败！', 'red')
        }
        await initUSBNetworkType()
    } catch (e) {
        // createToast(e.message)
    }
}