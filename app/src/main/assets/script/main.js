let smsSender = null
let statusShowList = localStorage.getItem('statusShowList') ? JSON.parse(localStorage.getItem('statusShowList')) : null

//初始化statusShowList为全选
if (!statusShowList) {
    const dicList = document.querySelectorAll('#DIC_LIST input')
    const list = []
    for (let item of dicList) {
        if (!item) continue
        const name = item.getAttribute('data-name')
        item.checked = true
        list.push(name)
    }
    statusShowList = list
    localStorage.setItem('statusShowList', JSON.stringify(list))
} else {
    const dicList = document.querySelectorAll('#DIC_LIST input')
    for (let item of dicList) {
        if (!item) continue
        const name = item.getAttribute('data-name')
        if (statusShowList.includes(name)) {
            item.checked = true
        } else {
            item.checked = false
        }
    }
}

function notNullOrundefinedOrIsShow(obj, dicName, flag = false) {
    let isNumber = typeof obj[dicName] === 'number'
    if (isNumber) {
        //如果是数字类型，直接返回
        return statusShowList.includes(dicName) || flag
    }
    let isReadable = obj[dicName] != null && obj[dicName] != undefined && obj[dicName] != ''
    //这里需要遍历一下是否显示的字段
    return isReadable && statusShowList.includes(dicName)
}

function copyText(e) {
    const text = e.target.innerText;
    if (navigator.clipboard && typeof navigator.clipboard.writeText === 'function') {
        // 浏览器支持
        navigator.clipboard.writeText(text).then(() => {
            createToast('已经复制到剪贴板！', 'green')
        }).catch(err => {
            createToast('复制失败', 'red')
        });
    } else {
        // 创建text area
        let textArea = document.createElement("textarea");
        textArea.value = text;
        // 使text area不在viewport，同时设置不可见
        textArea.style.position = "absolute";
        textArea.style.opacity = 0;
        textArea.style.left = "-999999px";
        textArea.style.top = "-999999px";
        document.body.appendChild(textArea);
        textArea.focus();
        textArea.select();
        return new Promise((res, rej) => {
            // 执行复制命令并移除文本框
            document.execCommand('copy') ? res() : rej();
            textArea.remove();
        }).then(() => {
            createToast('已经复制到剪贴板！', 'green')
        }).catch(() => {
            createToast('复制失败', 'red')
        });
    }
}

//按照信号dbm强度绘制信号强度栏(-113到-51)
function kano_parseSignalBar(val, min = -125, max = -81, step1 = -90, step2 = -100) {
    let strength = Number(val)
    strength = strength > max ? max : strength // 信号强度不能大于-51
    strength = strength < min ? min : strength // 信号强度不能小于-113
    //信号强度范围-113到-51
    const bar = document.createElement('span')
    const strengths = Array.from({ length: Math.abs((min - max)) + 1 }, (_, i) => min + i); // -113 到 -51
    const index = strengths.findIndex(i => i >= strength) // 找到对应的索引
    const percent = (index / strengths.length) * 100 // 计算百分比
    const progress = document.createElement('span')
    const text = document.createElement('span')

    text.innerHTML =  Number(val)
    bar.className = 'signal_bar'
    text.className = 'text'
    progress.className = 'signal_bar_progress'
    progress.style.transition = 'all 0.5s'
    progress.style.width = `${percent}%`
    progress.style.opacity = '.6'

    if (strength >= step1) {
        progress.style.backgroundColor = 'green';
    } else if (strength >= step2) {
        progress.style.backgroundColor = 'orange';
    } else {
        progress.style.backgroundColor = 'red';
    }

    bar.appendChild(progress)
    bar.appendChild(text)
    return bar.outerHTML
}

function kano_getSignalEmoji(strength) {
    const signals = ["📶 ⬜⬜⬜⬜", "📶 🟨⬜⬜⬜", "📶 🟩🟨⬜⬜", "📶 🟩🟩🟨⬜", "📶 🟩🟩🟩🟨", "📶 🟩🟩🟩🟩"];
    return signals[Math.max(0, Math.min(strength, 5))]; // 确保输入在 0-5 之间
}
function kano_formatTime(seconds) {
    if (seconds < 60) {
        return `${seconds} 秒`;
    } else if (seconds < 3600) {
        return `${(seconds / 60).toFixed(1)} 分钟`;
    } else {
        return `${(seconds / 3600).toFixed(1)} 小时`;
    }
}
function formatBytes(bytes) {
    if (bytes === 0) return '0 Byte';

    const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];
    const i = Math.floor(Math.log(bytes) / Math.log(1024));
    return (bytes / Math.pow(1024, i)).toFixed(2) + ' ' + sizes[i];
}
// KANO_INTERFACE.setValue("hello world")
function decodeBase64(base64String) {
    // 将Base64字符串分成每64个字符一组
    const padding = base64String.length % 4 === 0 ? 0 : 4 - (base64String.length % 4)
    base64String += '='.repeat(padding)

    // 使用atob()函数解码Base64字符串
    const binaryString = window.atob(base64String)

    // 将二进制字符串转换为TypedArray
    const bytes = new Uint8Array(binaryString.length)
    for (let i = 0; i < binaryString.length; i++) bytes[i] = binaryString.charCodeAt(i)

    // 将TypedArray转换为字符串
    return new TextDecoder('utf-8').decode(bytes)
}

function createToast(text, color, delay = 3000) {
    const toastEl = document.createElement('div')
    toastEl.style.position = 'fixed'
    toastEl.style.zIndex = '114514'
    toastEl.style.padding = '10px'
    toastEl.style.top = '20px'
    toastEl.style.right = '50%'
    toastEl.style.transform = 'translateX(50%)'
    toastEl.style.color = color || 'while'
    toastEl.style.backgroundColor = 'var(--dark-card-bg)'
    toastEl.style.transition = `opacity .2s`
    toastEl.style.opacity = `0`
    toastEl.style.boxShadow = '0 0 10px 0 #87ceeb70'
    toastEl.style.fontWeight = 'bold'
    toastEl.style.backdropFilter = 'blur(10px)'
    toastEl.style.borderRadius = '6px'
    toastEl.innerHTML = text;
    const id = 'toastkano'
    toastEl.setAttribute('class', id);
    const toasts = document.querySelectorAll('.toastkano')
    document.body.appendChild(toastEl)
    setTimeout(() => {
        toastEl.style.opacity = `1`
    }, 0);
    if (toasts.length) {
        const top = toasts[toasts.length - 1].getBoundingClientRect().top
        const height = toastEl.getBoundingClientRect().height
        toastEl.style.top = top + height + 10 + 'px'
    }
    let timer = null
    setTimeout(() => {
        toastEl.style.opacity = `0`
        clearTimeout(timer)
        timer = setTimeout(() => {
            document.body.removeChild(toastEl)
        }, 200);
    }, delay);
}
function closeModal(txt, time = 300) {
    if (txt == '#smsList') smsSender && smsSender()
    let el = document.querySelector(txt)
    if (!el) return
    el.style.opacity = 0
    setTimeout(() => {
        el.style.display = 'none'
    }, time)
}
function showModal(txt, time = 300) {
    let el = document.querySelector(txt)
    if (!el) return
    el.style.opacity = 0
    el.style.display = ''
    setTimeout(() => {
        el.style.opacity = 1
    }, 10)
}

const debounce = (func, delay) => {
    let timer;
    return function (...args) {
        if (timer) clearTimeout(timer);
        timer = setTimeout(() => {
            func.apply(this, args);
        }, delay);
    };
};
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
        handlerPerformaceStatus()
        initNetworktype()
        initSMBStatus()
        initLightStatus()
        initBandForm()
        initUSBNetworkType()
        initWIFISwitch()
        rebootDeviceBtnInit()
    }
    catch {
        createToast('登录失败，请检查网络！', 'red')
    }
}, 200)

function requestInterval(callback, interval) {
    let lastTime = 0;
    let timeoutId = null;

    function loop(timestamp) {
        if (!lastTime) lastTime = timestamp; // 初始化上次时间
        const delta = timestamp - lastTime; // 计算时间差

        if (delta >= interval) {
            callback(); // 执行任务
            lastTime = timestamp; // 更新上次时间
        }

        timeoutId = requestAnimationFrame(loop); // 继续请求下一帧
    }

    timeoutId = requestAnimationFrame(loop); // 启动动画循环

    // 返回清除函数
    return () => cancelAnimationFrame(timeoutId);
}

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
        list.innerHTML = ` <li> <h2 style="padding: 30px;text-align:center;height:100vh;">没有短信</h2></li >`
    }
}


let StopStatusRenderTimer = null
let handlerStatusRender = async (flag = false) => {
    const res = await getUFIData()
    if (!res) {
        // out()
        if (flag) {
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
        const status = document.querySelector('#STATUS')
        const current_cell = document.querySelector('#CURRENT_CELL')
        let html = ''

        if (current_cell) {
            current_cell.innerHTML = '<i>当前连接</i><br/>'
            current_cell.innerHTML += `
            ${notNullOrundefinedOrIsShow(res, 'Lte_fcn') ? `<span>EARFCN: ${res.Lte_fcn}</span>` : ''}
            ${notNullOrundefinedOrIsShow(res, 'Lte_pci') ? `<span>&nbsp;PCI: ${res.Lte_pci}</span>` : ''}
            ${notNullOrundefinedOrIsShow(res, 'lte_rsrq') ? `<span>&nbsp;RSRQ: ${res.lte_rsrq}</span>` : ''}
            ${notNullOrundefinedOrIsShow(res, 'lte_rsrp') ? `<div style="display: flex;padding-bottom:2px;align-items: center;">RSRP:&nbsp; ${kano_parseSignalBar(res.lte_rsrp)}</div>` : ''}
            ${notNullOrundefinedOrIsShow(res, 'Lte_snr') ? `<div style="display: flex;align-items: center;">SINR:&nbsp; ${kano_parseSignalBar(res.Lte_snr, -11, 31, 13, 5)}</div>` : ''}
            ${notNullOrundefinedOrIsShow(res, 'Nr_fcn') ? `<span>EARFCN: ${res.Nr_fcn}</span>` : ''}
            ${notNullOrundefinedOrIsShow(res, 'Nr_pci') ? `<span>&nbsp;PCI: ${res.Nr_pci}</span>` : ''}
            ${notNullOrundefinedOrIsShow(res, 'nr_rsrq') ? `<span>RSRQ: ${res.nr_rsrq}</span>` : ''}
            ${notNullOrundefinedOrIsShow(res, 'Z5g_rsrp') ? `<div style="display: flex;padding-bottom:2px;align-items: center;">RSRP:&nbsp; ${kano_parseSignalBar(res.Z5g_rsrp)}</div>` : ''}
            ${notNullOrundefinedOrIsShow(res, 'Nr_snr') ? `<div style="display: flex;align-items: center;">SINR:&nbsp; ${kano_parseSignalBar(res.Nr_snr, -11, 31, 13, 5)}</div>` : ''}
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
            monthly_tx_bytes: `${notNullOrundefinedOrIsShow(res, 'monthly_tx_bytes') || notNullOrundefinedOrIsShow(res, 'monthly_rx_bytes') ? `<strong onclick="copyText(event)"  class="blue">已用流量：<span class="red">${Number((res.monthly_tx_bytes + res.monthly_rx_bytes) / 1024 / 1024 / 1024).toFixed(2)} GB</span>${res.data_volume_limit_size ? '&nbsp;<span style="color:white">/</span>&nbsp;总流量：' + res.data_volume_limit_size.split('_')[0] + ' GB' : ''}</strong>` : ''}`,
            daily_data: `${notNullOrundefinedOrIsShow(res, 'daily_data') ? `<strong onclick="copyText(event)"  class="blue">当日流量：${res.daily_data}</strong>` : ''}`,
            internal_available_storage: `${notNullOrundefinedOrIsShow(res, 'internal_available_storage') || notNullOrundefinedOrIsShow(res, 'internal_total_storage') ? `<strong onclick="copyText(event)"  class="blue">内部存储：${res.internal_available_storage} / ${res.internal_total_storage}</strong>` : ''}`,
            external_available_storage: `${notNullOrundefinedOrIsShow(res, 'external_available_storage') || notNullOrundefinedOrIsShow(res, 'external_total_storage') ? `<strong onclick="copyText(event)"  class="blue">SD卡：${res.external_available_storage} / ${res.external_total_storage}</strong>` : ''}`,
            realtime_rx_thrpt: `${notNullOrundefinedOrIsShow(res, 'realtime_tx_thrpt') || notNullOrundefinedOrIsShow(res, 'realtime_rx_thrpt') ? `<strong onclick="copyText(event)"  class="blue">当前网速: ⬇️ ${formatBytes(Number((res.realtime_rx_thrpt)))}/S ⬆️ ${formatBytes(Number((res.realtime_tx_thrpt)))}/S</strong>` : ''}`,
        }
        let statusHtml_net = {
            lte_rsrp: `${notNullOrundefinedOrIsShow(res, 'lte_rsrp') ? `<strong onclick="copyText(event)"  class="green">4G接收功率：${kano_parseSignalBar(res.lte_rsrp)}</strong>` : ''}`,
            Lte_snr: `${notNullOrundefinedOrIsShow(res, 'Lte_snr') ? `<strong onclick="copyText(event)"  class="blue">4G SINR：${kano_parseSignalBar(res.Lte_snr, -11, 31, 13, 5)}</strong>` : ''}`,
            Lte_bands: `${notNullOrundefinedOrIsShow(res, 'Lte_bands') ? `<strong onclick="copyText(event)"  class="blue">4G 注册频段：B${res.Lte_bands}</strong>` : ''}`,
            Lte_fcn: `${notNullOrundefinedOrIsShow(res, 'Lte_fcn') ? `<strong onclick="copyText(event)"  class="green">4G 频点：${res.Lte_fcn}</strong>` : ''}`,
            Lte_bands_widths: `${notNullOrundefinedOrIsShow(res, 'Lte_bands_widths') ? `<strong onclick="copyText(event)"  class="green">4G 频宽：${res.Lte_bands_widths}</strong>` : ''}`,
            Lte_pci: `${notNullOrundefinedOrIsShow(res, 'Lte_pci') ? `<strong onclick="copyText(event)"  class="blue">4G PCI：${res.Lte_pci}</strong>` : ''}`,
            lte_rsrq: `${notNullOrundefinedOrIsShow(res, 'lte_rsrq') ? `<strong onclick="copyText(event)"  class="blue">4G RSRQ：${res.lte_rsrq}</strong>` : ''}`,
            lte_rssi: `${notNullOrundefinedOrIsShow(res, 'lte_rssi') ? `<strong onclick="copyText(event)"  class="green">4G RSSI：${res.lte_rssi}</strong>` : ''}`,
            Lte_cell_id: `${notNullOrundefinedOrIsShow(res, 'Lte_cell_id') ? `<strong onclick="copyText(event)"  class="green">4G 小区ID：${res.Lte_cell_id}</strong>` : ''}`,
            Z5g_rsrp: `${notNullOrundefinedOrIsShow(res, 'Z5g_rsrp') ? `<strong onclick="copyText(event)"  class="green">5G接收功率：${kano_parseSignalBar(res.Z5g_rsrp)}</strong>` : ''}`,
            Nr_snr: `${notNullOrundefinedOrIsShow(res, 'Nr_snr') ? `<strong onclick="copyText(event)"  class="green">5G SINR：${kano_parseSignalBar(res.Nr_snr, -11, 31, 13, 5)}</strong>` : ''}`,
            Nr_bands: `${notNullOrundefinedOrIsShow(res, 'Nr_bands') ? `<strong onclick="copyText(event)"  class="green">5G 注册频段：N${res.Nr_bands}</strong>` : ''}`,
            Nr_fcn: `${notNullOrundefinedOrIsShow(res, 'Nr_fcn') ? `<strong onclick="copyText(event)"  class="blue">5G 频点：${res.Nr_fcn}</strong>` : ''}`,
            Nr_bands_widths: `${notNullOrundefinedOrIsShow(res, 'Nr_bands_widths') ? `<strong onclick="copyText(event)"  class="blue">5G 频宽：${res.Nr_bands_widths}</strong>` : ''}`,
            Nr_pci: `${notNullOrundefinedOrIsShow(res, 'Nr_pci') ? `<strong onclick="copyText(event)"  class="green">5G PCI：${res.Nr_pci}</strong>` : ''}`,
            nr_rsrq: `${notNullOrundefinedOrIsShow(res, 'nr_rsrq') ? `<strong onclick="copyText(event)"  class="green">5G RSRQ：${res.nr_rsrq}</strong>` : ''}`,
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
        statusShowList.forEach(item => {
            if (statusHtml_base[item]) {
                html += statusHtml_base[item]
            }

        })
        html += `</p></li>`
        html += `<div class="title" style="margin: 6px 0;"><b>信号参数</b></div>`

        html += `<li style="padding-top: 15px;"><p>`
        statusShowList.forEach(item => {
            if (statusHtml_net[item]) {
                html += statusHtml_net[item]
            }

        })
        html += `</p></li>`
        html += `<div class="title" style="margin: 6px 0;"><b>设备属性</b></div>`

        html += `<li style="padding-top: 15px;"><p>`
        statusShowList.forEach(item => {
            if (statusHtml_other[item]) {
                html += statusHtml_other[item]
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
            res = await (await postData(cookie, {
                goformId: 'USB_PORT_SETTING',
                usb_port_switch: res.usb_port_switch == '1' ? '0' : '1'
            })).json()
            if (res.result == 'success') {
                createToast('操作成功！', 'green')
                await handlerADBStatus()
            } else {
                createToast('操作失败！', 'red')
            }
        } catch (e) {
            createToast(e.message)
        }
    }
    btn.innerHTML = res.usb_port_switch == '1' ? '关闭USB调试' : '开启USB调试'
    btn.style.backgroundColor = res.usb_port_switch == '1' ? '#018AD8' : ''

}
handlerADBStatus()

//检查性能模式状态
let handlerPerformaceStatus = async () => {
    const btn = document.querySelector('#PERF')
    if (!initRequestData()) {
        btn.style.backgroundColor = '#80808073'
        return null
    }
    let res = await getData(new URLSearchParams({
        cmd: 'performance_mode'
    }))
    btn.innerHTML = res.performance_mode == '1' ? '关闭性能模式' : '开启性能模式'
    btn.style.backgroundColor = res.performance_mode == '1' ? '#018AD8' : ''
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
            res = await (await postData(cookie, {
                goformId: 'PERFORMANCE_MODE_SETTING',
                performance_mode: res.performance_mode == '1' ? '0' : '1'
            })).json()
            if (res.result == 'success') {
                createToast('操作成功，重启生效！', 'green')
                await handlerPerformaceStatus()
            } else {
                createToast('操作失败！', 'red')
            }
        } catch (e) {
            createToast(e.message)
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
    handlerPerformaceStatus()
    initNetworktype()
    initSMBStatus()
    initLightStatus()
    initBandForm()
    rebootDeviceBtnInit()
    initUSBNetworkType()
    initWIFISwitch()
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
        createToast(e.message)
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
        createToast(e.message)
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
        } else {
            createToast('操作失败！', 'red')
        }
        await initWIFISwitch()
    } catch (e) {
        createToast(e.message)
    }
}

let initSMBStatus = async () => {
    const el = document.querySelector('#SMB')
    if (!initRequestData() || !el) {
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
            res = await (await postData(cookie, {
                goformId: 'SAMBA_SETTING',
                samba_switch: res.samba_switch == '1' ? '0' : '1'
            })).json()
            if (res.result == 'success') {
                createToast('操作成功！', 'green')
            } else {
                createToast('操作失败！', 'red')
            }
            await initSMBStatus()
        } catch (e) {
            createToast(e.message)
        }
    }
    el.innerHTML = res.samba_switch == '1' ? '关闭SMB' : '开启SMB'
    el.style.backgroundColor = res.samba_switch == '1' ? '#018AD8' : ''
}
initSMBStatus()

let initLightStatus = async () => {
    const el = document.querySelector('#LIGHT')
    if (!initRequestData() || !el) {
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
            res = await (await postData(cookie, {
                goformId: 'INDICATOR_LIGHT_SETTING',
                indicator_light_switch: res.indicator_light_switch == '1' ? '0' : '1'
            })).json()
            if (res.result == 'success') {
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
    el.style.backgroundColor = res.indicator_light_switch == '1' ? '#018AD8' : ''
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
                        <td>${rsrp}</td>
                        <td>${rsrq}</td>
                        <td>${sinr}</td>
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
initCellInfo()
requestInterval(() => initCellInfo(), 1500)

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
        target.style.backgroundColor = '#80808073'
        target.onclick = null
        return null
    }
    target.style.backgroundColor = ''
    target.onclick = rebootDevice
}
rebootDeviceBtnInit()

//字段显示隐藏
document.querySelector("#DICTIONARY").onclick = (e) => {
    showModal('#dictionaryModal')
}

document.querySelector('#DIC_LIST')?.addEventListener('click', (e) => {
    let target = e.target
    e.stopPropagation()
    e.stopImmediatePropagation()
    if (target.id == 'DIC_LIST') {
        return
    }
    let inputEl = null
    if (target.tagName == 'label' || target.tagName == 'LABEL') {
        return
    } else {
        inputEl = target
    }
    let id = inputEl.getAttribute('data-name')
    if (inputEl.checked) {
        statusShowList.push(id)
    } else {
        statusShowList = statusShowList.filter(item => item != id)
    }
    //保存
    localStorage.setItem('statusShowList', JSON.stringify(statusShowList))
}, false)

let resetShowListBtnCount = 1
let resetShowListTimer = null
let resetShowList = (e) => {
    const target = e.target
    resetShowListTimer && clearTimeout(resetShowListTimer)
    if (resetShowListBtnCount == 1) target.innerHTML = "确定重置？"
    if (resetShowListBtnCount >= 2) {
        localStorage.removeItem('statusShowList');
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
        StopStatusRenderTimer = requestInterval(() => handlerStatusRender(), 800)
    } else {
        e.target.innerHTML = '开始刷新'
        createToast('已停止刷新', 'green')
        StopStatusRenderTimer && StopStatusRenderTimer()
    }
}