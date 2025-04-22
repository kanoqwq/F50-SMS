function DragList(listId, callback) {
    if (!listId) return null
    const list = document.querySelector(listId);
    let currentLi = null;
    let offsetY = 0;
    let placeholder = document.createElement("li");
    placeholder.className = "moving";
    placeholder.innerHTML = "&nbsp;";
    placeholder.style.height = "30px";

    // -------- PC 拖拽逻辑 --------
    list.addEventListener("dragstart", (e) => {
        currentLi = e.target;
        setTimeout(() => currentLi.classList.add("moving"), 0);
    });

    list.addEventListener("dragover", (e) => e.preventDefault());

    //元素拖拽到目标元素上时触发
    list.addEventListener("dragenter", (e) => {
        if (e.target === currentLi || e.target === list) return;

        let items = [...list.children];
        let currIdx = items.indexOf(currentLi);
        let targetIdx = items.indexOf(e.target);

        try {
            if (currIdx < targetIdx) {
                // 如果 newNode 已经在 DOM 中，它会被自动移除再插入到新位置。
                list?.insertBefore(currentLi, e.target.nextSibling);
            } else {
                list?.insertBefore(currentLi, e.target);
            }
        } catch { }
    });

    list.addEventListener("dragend", () => {
        if (currentLi) currentLi.classList.remove("moving");
        currentLi = null;
        callback && callback(list)
    });

    // -------- 移动端 touch 拖拽逻辑 --------
    list.addEventListener("touchstart", (e) => {
        const target = e.target;
        if (target.tagName.toLowerCase() !== "li") return;

        e.preventDefault(); // 阻止页面滚动
        currentLi = target;

        const rect = target.getBoundingClientRect();
        offsetY = e.touches[0].clientY - rect.top;

        // 插入占位符
        placeholder.style.height = `${target.offsetHeight}px`;
        list.insertBefore(placeholder, currentLi.nextSibling);

        // 设为拖拽样式
        currentLi.classList.add("dragging");
        currentLi.style.left = "0px";
        //   currentLi.style.width = "100%";
        currentLi.style.top = `${e.touches[0].clientY - offsetY - list.getBoundingClientRect().top}px`;
    });

    list.addEventListener("touchmove", (e) => {
        if (!currentLi) return;

        const touchY = e.touches[0].clientY;
        const ulTop = list.getBoundingClientRect().top;

        currentLi.style.top = `${touchY - offsetY - ulTop}px`;

        let liItems = [...list.querySelectorAll("li")].filter(
            (li) => li !== currentLi && li !== placeholder
        );

        for (let li of liItems) {
            let rect = li.getBoundingClientRect();
            if (touchY > rect.top && touchY < rect.bottom) {
                if (touchY < rect.top + rect.height / 2) {
                    list.insertBefore(placeholder, li);
                } else {
                    list.insertBefore(placeholder, li.nextSibling);
                }
                break;
            }
        }
    });

    list.addEventListener("touchend", () => {
        if (!currentLi) return;
        currentLi.classList.remove("dragging");
        currentLi.style = "";
        list.insertBefore(currentLi, placeholder);
        placeholder.remove();
        currentLi = null;
        callback && callback(list)
    });
}
