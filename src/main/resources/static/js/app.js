// Navegación, usuario seleccionado y refresco de la pantalla actual.
'use strict';

const SECCIONES = [
    ['dashboard', 'Dashboard'], ['usuarios', 'Usuarios'], ['billeteras', 'Billeteras'],
    ['transacciones', 'Transacciones'], ['programadas', 'Programadas'], ['puntos', 'Puntos y beneficios'],
    ['notificaciones', 'Notificaciones'], ['auditoria', 'Auditoría'], ['analitica', 'Analítica y grafo'],
    ['benchmark', 'Benchmark']
];

const App = {
    seccion: 'dashboard',
    usuario: null,

    async iniciar() {
        const menu = document.getElementById('menu');
        SECCIONES.forEach(([id, titulo]) => menu.append(h('a', { href: '#' + id, 'data-seccion': id }, titulo)));
        window.addEventListener('hashchange', () => App.ir(location.hash.slice(1)));
        document.getElementById('usuarioActual').addEventListener('change', e => App.seleccionar(e.target.value || null));
        document.getElementById('modalCerrar').addEventListener('click', Ui.cerrarModal);
        document.getElementById('botonMenu').addEventListener('click',
            () => document.getElementById('sidebar').classList.toggle('abierta'));
        document.addEventListener('keydown', e => { if (e.key === 'Escape') Ui.cerrarModal(); });
        try {
            App.usuario = sessionStorage.getItem('fintech.usuario');
        } catch (e) {
            App.usuario = null;
        }
        await App.recargarUsuarios(App.usuario, true);
        await App.ir(location.hash.slice(1) || 'dashboard');
    },

    async recargarUsuarios(seleccionar, sinRefrescar) {
        const select = document.getElementById('usuarioActual');
        let usuarios = [];
        try {
            usuarios = await Api.usuarios();
        } catch (e) {
            Ui.error(e);
        }
        const existe = usuarios.some(u => u.identificacion === seleccionar);
        select.replaceChildren(h('option', { value: '' }, '— ninguno —'),
            ...usuarios.map(u => h('option', { value: u.identificacion, selected: u.identificacion === seleccionar },
                u.identificacion + ' · ' + u.nombre)));
        await App.seleccionar(existe ? seleccionar : null, sinRefrescar);
    },

    async seleccionar(id, sinRefrescar) {
        App.usuario = id || null;
        document.getElementById('usuarioActual').value = App.usuario || '';
        try {
            if (App.usuario) sessionStorage.setItem('fintech.usuario', App.usuario);
            else sessionStorage.removeItem('fintech.usuario');
        } catch (e) {
            // almacenamiento no disponible: la selección dura solo esta página
        }
        if (!sinRefrescar) await App.refrescar();
    },

    async ir(seccion) {
        Ui.cerrarModal();
        if (!Pantallas[seccion]) seccion = 'dashboard';
        App.seccion = seccion;
        document.querySelectorAll('#menu a').forEach(a => a.classList.toggle('activo', a.dataset.seccion === seccion));
        document.getElementById('titulo').textContent = SECCIONES.find(s => s[0] === seccion)[1];
        document.getElementById('sidebar').classList.remove('abierta');
        await App.refrescar();
    },

    async refrescar() {
        const contenido = document.getElementById('contenido');
        Ui.cargando(true);
        try {
            contenido.replaceChildren(await Pantallas[App.seccion](App.usuario));
        } catch (e) {
            Ui.error(e);
            contenido.replaceChildren(h('section', { clase: 'panel' }, h('p', { clase: 'vacio' },
                'No se pudo cargar la sección: ' + e.message)));
        } finally {
            Ui.cargando(false);
        }
    }
};

document.addEventListener('DOMContentLoaded', App.iniciar);
