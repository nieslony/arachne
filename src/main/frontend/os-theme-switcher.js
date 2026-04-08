function applyTheme() {
    let theme = matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
    document.documentElement.setAttribute("theme", theme);

    var link = document.querySelector("link[rel~='icon']");

    const curFavivonLinks = document.querySelectorAll("link[rel~='icon']");
    curFavivonLinks.forEach(link => link.remove());

    const sizes = [16, 32, 48, 64, 120, 144, 152, 180, 192, 512];
    sizes.forEach(size => {
        link = document.createElement('link');
        link.rel = 'icon';
        link.href = "icons/arachne_dark.png?size=" + size;
        link.sizes = "" + size + "x" + size;
        document.head.appendChild(link);
    });
}

matchMedia("(prefers-color-scheme: dark)").addEventListener("change", applyTheme);
applyTheme();
