// Utilidades de interfaz: creación segura de nodos, tablas, modales, mensajes y cargador.
'use strict';

function h(etiqueta, atributos, ...hijos) {
    const nodo = document.createElement(etiqueta);
    Object.entries(atributos || {}).forEach(([k, v]) => {
        if (v === undefined || v === null || v === false) return;
        if (k.startsWith('on')) nodo.addEventListener(k.slice(2), v);
        else if (k === 'clase') nodo.className = v;
        else nodo.setAttribute(k, v === true ? '' : v);
    });
    hijos.flat().forEach(hijo => {
        if (hijo === undefined || hijo === null || hijo === false) return;
        nodo.append(hijo instanceof Node ? hijo : document.createTextNode(String(hijo)));
    });
    return nodo;
}

const Formato = {
    // Solo presentación: los montos llegan calculados por el servidor.
    dinero: v => v === null || v === undefined ? '—'
        : '$ ' + Number(v).toLocaleString('es-CO', { minimumFractionDigits: 2, maximumFractionDigits: 2 }),
    fecha: v => v ? String(v).replace('T', ' ').slice(0, 16) : '—',
    insignia: v => h('span', { clase: 'insignia ' + v }, v)
};

function tabla(columnas, filas, vacio) {
    if (!filas || filas.length === 0) return h('p', { clase: 'vacio' }, vacio || 'Sin registros.');
    return h('table', {},
        h('thead', {}, h('tr', {}, columnas.map(c => h('th', {}, c.titulo)))),
        h('tbody', {}, filas.map(f => h('tr', {}, columnas.map(c => {
            const valor = c.valor(f);
            return h('td', { clase: c.num ? 'num' : '' }, valor);
        })))));
}

function tarjeta(etiqueta, valor) {
    return h('div', { clase: 'tarjeta' }, h('div', { clase: 'etiqueta' }, etiqueta), h('div', { clase: 'valor' }, valor));
}

function panel(titulo, ...contenido) {
    return h('section', { clase: 'panel' }, h('h2', {}, titulo), ...contenido);
}

const Ui = {
    mensaje(texto, error, codigo) {
        const m = h('div', { clase: 'mensaje' + (error ? ' error' : ''), role: error ? 'alert' : 'status' },
            codigo ? h('span', { clase: 'codigo' }, codigo) : null, texto);
        document.getElementById('mensajes').append(m);
        setTimeout(() => m.remove(), error ? 7000 : 3500);
    },
    error(e) {
        Ui.mensaje(e.message || String(e), true, e.codigo);
    },
    cargando(activo) {
        document.getElementById('cargando').hidden = !activo;
    },
    /** Ejecuta una acción asíncrona deshabilitando el botón: evita envíos dobles. */
    async conBloqueo(boton, accion) {
        if (boton && boton.disabled) return undefined;
        if (boton) boton.disabled = true;
        Ui.cargando(true);
        try {
            return await accion();
        } catch (e) {
            Ui.error(e);
            return undefined;
        } finally {
            Ui.cargando(false);
            if (boton) boton.disabled = false;
        }
    },
    cerrarModal() {
        document.getElementById('modal').hidden = true;
        document.getElementById('modalCuerpo').replaceChildren();
    },
    /**
     * Abre un formulario modal. campos: [{nombre, etiqueta, tipo, opciones, requerido, valor, paso}].
     * enviar(datos) devuelve una promesa; si se resuelve, se cierra el modal.
     */
    formulario(titulo, campos, textoBoton, enviar) {
        const boton = h('button', { type: 'submit' }, textoBoton);
        const form = h('form', { clase: 'formulario', novalidate: true },
            campos.map(c => h('label', {}, c.etiqueta, c.tipo === 'select'
                ? h('select', { name: c.nombre, required: c.requerido }, c.opciones.map(o =>
                    h('option', { value: o.valor ?? o, selected: (o.valor ?? o) === c.valor }, o.texto ?? o)))
                : h('input', { name: c.nombre, type: c.tipo || 'text', required: c.requerido, value: c.valor,
                    step: c.paso, min: c.minimo, placeholder: c.ayuda }))),
            h('div', { clase: 'acciones' }, boton,
                h('button', { type: 'button', clase: 'secundario', onclick: Ui.cerrarModal }, 'Cancelar')));
        form.addEventListener('submit', async ev => {
            ev.preventDefault();
            const datos = {};
            new FormData(form).forEach((v, k) => { datos[k] = v === '' ? null : v; });
            const ok = await Ui.conBloqueo(boton, async () => { await enviar(datos); return true; });
            if (ok) Ui.cerrarModal();
        });
        document.getElementById('modalTitulo').textContent = titulo;
        document.getElementById('modalCuerpo').replaceChildren(form);
        document.getElementById('modal').hidden = false;
        const primero = form.querySelector('input,select');
        if (primero) primero.focus();
    },
    /** Botón que ejecuta una acción con bloqueo y luego refresca la pantalla. */
    boton(texto, accion, clase) {
        const b = h('button', { type: 'button', clase: clase || '' }, texto);
        b.addEventListener('click', () => Ui.conBloqueo(b, accion));
        return b;
    }
};
