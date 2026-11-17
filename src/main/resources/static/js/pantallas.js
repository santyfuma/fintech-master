// Las diez pantallas. Cada una devuelve un nodo; usan el usuario seleccionado en la barra superior.
'use strict';

const TIPOS_BILLETERA = ['AHORRO', 'GASTOS_DIARIOS', 'COMPRAS', 'TRANSPORTE', 'INVERSION', 'SUSCRIPCIONES'];
const RIESGOS = ['', 'BAJO', 'MEDIO', 'ALTO'];

function requiereUsuario() {
    return h('section', { clase: 'panel' }, h('p', { clase: 'vacio' },
        'Seleccione un usuario en la barra superior (o registre uno en "Usuarios").'));
}

function numero(v) {
    // Se envía como número JSON; el servidor lo convierte a BigDecimal y valida.
    return v === null ? null : Number(v);
}

const COLUMNAS_TRANSACCION = [
    { titulo: 'Id', valor: t => t.id },
    { titulo: 'Fecha', valor: t => Formato.fecha(t.fecha) },
    { titulo: 'Tipo', valor: t => t.tipo },
    { titulo: 'Valor', valor: t => Formato.dinero(t.valor), num: true },
    { titulo: 'Origen', valor: t => t.billeteraOrigen || '—' },
    { titulo: 'Destino', valor: t => t.billeteraDestino || '—' },
    { titulo: 'Estado', valor: t => Formato.insignia(t.estado) },
    { titulo: 'Puntos', valor: t => t.puntosGenerados, num: true },
    { titulo: 'Riesgo', valor: t => Formato.insignia(t.nivelRiesgo) }
];

function opcionesBilleteras(billeteras) {
    return billeteras.filter(b => b.estado === 'ACTIVA')
        .map(b => ({ valor: b.codigo, texto: b.codigo + ' · ' + b.nombre + ' (' + Formato.dinero(b.saldo) + ')' }));
}

const Pantallas = {

    async dashboard(u) {
        const usuarios = await Api.usuarios();
        const nodos = [h('div', { clase: 'tarjetas' }, tarjeta('Usuarios registrados', usuarios.length))];
        if (!u) return h('div', {}, nodos, requiereUsuario());
        const [usuario, billeteras, transacciones, notificaciones, total] = await Promise.all([
            Api.puntos(u), Api.billeteras(u), Api.transaccionesUsuario(u), Api.notificaciones(u), Api.saldoTotal(u)]);
        const exitosas = transacciones.filter(t => t.estado === 'EXITOSA').length;
        nodos.push(h('div', { clase: 'tarjetas' },
            tarjeta('Saldo total', Formato.dinero(total.saldoTotal)),
            tarjeta('Actividad', transacciones.length + ' movimientos (' + exitosas + ' exitosos)'),
            tarjeta('Nivel', usuario.nivel),
            tarjeta('Puntos', usuario.puntosAcumulados),
            tarjeta('Para ' + (usuario.siguienteNivel || 'máximo'),
                usuario.puntosParaSiguienteNivel === null ? 'Nivel máximo' : usuario.puntosParaSiguienteNivel + ' pts'),
            tarjeta('Billeteras', billeteras.length),
            tarjeta('Notificaciones', notificaciones.length)));
        nodos.push(panel('Billeteras', tabla([
            { titulo: 'Código', valor: b => b.codigo }, { titulo: 'Nombre', valor: b => b.nombre },
            { titulo: 'Tipo', valor: b => b.tipo }, { titulo: 'Saldo', valor: b => Formato.dinero(b.saldo), num: true },
            { titulo: 'Estado', valor: b => Formato.insignia(b.estado) }], billeteras)));
        nodos.push(panel('Últimas transacciones', tabla(COLUMNAS_TRANSACCION, transacciones.slice(-5).reverse())));
        return h('div', {}, nodos);
    },

    async usuarios(u) {
        const filtros = h('div', { clase: 'filtros' });
        const minimo = h('input', { type: 'number', min: 0, placeholder: 'mín' });
        const maximo = h('input', { type: 'number', min: 0, placeholder: 'máx' });
        const nivel = h('select', {}, ['', 'BRONCE', 'PLATA', 'ORO', 'PLATINO'].map(n => h('option', { value: n }, n || 'Todos')));
        const contenedor = h('div');
        const pintar = lista => contenedor.replaceChildren(tabla([
            { titulo: 'Identificación', valor: x => x.identificacion }, { titulo: 'Nombre', valor: x => x.nombre },
            { titulo: 'Correo', valor: x => x.correo }, { titulo: 'Puntos', valor: x => x.puntosAcumulados, num: true },
            { titulo: 'Nivel', valor: x => Formato.insignia(x.nivel) },
            { titulo: '', valor: x => h('div', { clase: 'acciones' },
                    Ui.boton('Seleccionar', async () => App.seleccionar(x.identificacion), 'pequeno secundario'),
                    Ui.boton('Editar', async () => editar(x), 'pequeno secundario'),
                    Ui.boton('Eliminar', async () => {
                        if (!confirm('¿Eliminar al usuario ' + x.identificacion + '?')) return;
                        await Api.eliminarUsuario(x.identificacion);
                        Ui.mensaje('Usuario eliminado');
                        await App.recargarUsuarios();
                    }, 'pequeno peligro')) }], lista));
        const editar = x => Ui.formulario('Editar usuario ' + x.identificacion, [
            { nombre: 'nombre', etiqueta: 'Nombre', valor: x.nombre, requerido: true },
            { nombre: 'correo', etiqueta: 'Correo', tipo: 'email', valor: x.correo, requerido: true }], 'Guardar',
            async d => { await Api.actualizarUsuario(x.identificacion, d); Ui.mensaje('Usuario actualizado'); await App.refrescar(); });
        filtros.append(h('label', {}, 'Puntos', h('div', { clase: 'acciones' }, minimo, maximo)), h('label', {}, 'Nivel', nivel),
            Ui.boton('Filtrar', async () => {
                if (nivel.value) pintar(await Api.porNivel(nivel.value));
                else pintar(await Api.porPuntos(minimo.value, maximo.value));
            }, 'secundario'));
        pintar(await Api.porPuntos());
        const nuevo = Ui.boton('Registrar usuario', async () => Ui.formulario('Registrar usuario', [
            { nombre: 'identificacion', etiqueta: 'Identificación', requerido: true },
            { nombre: 'nombre', etiqueta: 'Nombre', requerido: true },
            { nombre: 'correo', etiqueta: 'Correo', tipo: 'email', requerido: true }], 'Registrar', async d => {
            const r = await Api.registrarUsuario(d);
            Ui.mensaje('Usuario ' + r.identificacion + ' registrado');
            await App.recargarUsuarios(r.identificacion);
        }));
        return panel(h('span', {}, 'Usuarios (ordenados por puntos)'), h('div', { clase: 'acciones' }, nuevo), filtros, contenedor);
    },

    async billeteras(u) {
        if (!u) return requiereUsuario();
        const billeteras = await Api.billeteras(u);
        const editar = b => Ui.formulario('Editar ' + b.codigo, [
            { nombre: 'nombre', etiqueta: 'Nombre', valor: b.nombre, requerido: true },
            { nombre: 'tipo', etiqueta: 'Tipo', tipo: 'select', opciones: TIPOS_BILLETERA, valor: b.tipo },
            { nombre: 'estado', etiqueta: 'Estado', tipo: 'select', opciones: ['ACTIVA', 'INACTIVA'], valor: b.estado }],
            'Guardar', async d => { await Api.actualizarBilletera(b.codigo, d); Ui.mensaje('Billetera actualizada'); await App.refrescar(); });
        const historial = h('div');
        const lista = tabla([
            { titulo: 'Código', valor: b => b.codigo }, { titulo: 'Nombre', valor: b => b.nombre },
            { titulo: 'Tipo', valor: b => b.tipo }, { titulo: 'Saldo', valor: b => Formato.dinero(b.saldo), num: true },
            { titulo: 'Estado', valor: b => Formato.insignia(b.estado) },
            { titulo: '', valor: b => h('div', { clase: 'acciones' },
                    Ui.boton('Historial', async () => historial.replaceChildren(panel('Historial de ' + b.codigo,
                        tabla(COLUMNAS_TRANSACCION, await Api.transaccionesBilletera(b.codigo)))), 'pequeno secundario'),
                    Ui.boton('Editar', async () => editar(b), 'pequeno secundario'),
                    b.estado === 'ACTIVA'
                        ? Ui.boton('Desactivar', async () => { await Api.desactivarBilletera(b.codigo); Ui.mensaje('Billetera desactivada'); await App.refrescar(); }, 'pequeno peligro')
                        : Ui.boton('Activar', async () => { await Api.activarBilletera(b.codigo); Ui.mensaje('Billetera activada'); await App.refrescar(); }, 'pequeno')) }],
            billeteras, 'El usuario no tiene billeteras.');
        const nueva = Ui.boton('Crear billetera', async () => Ui.formulario('Nueva billetera', [
            { nombre: 'nombre', etiqueta: 'Nombre', requerido: true },
            { nombre: 'tipo', etiqueta: 'Tipo', tipo: 'select', opciones: TIPOS_BILLETERA }], 'Crear', async d => {
            const b = await Api.crearBilletera(u, d);
            Ui.mensaje('Billetera ' + b.codigo + ' creada');
            await App.refrescar();
        }));
        return h('div', {}, panel('Billeteras del usuario ' + u, h('div', { clase: 'acciones' }, nueva), lista), historial);
    },

    async transacciones(u) {
        if (!u) return requiereUsuario();
        const [billeteras, transacciones] = await Promise.all([Api.billeteras(u), Api.transaccionesUsuario(u)]);
        const propias = opcionesBilleteras(billeteras);
        const exito = t => Ui.mensaje(t.tipo + ' ' + t.id + ' ' + t.estado.toLowerCase() + ' por ' + Formato.dinero(t.valor));
        const acciones = h('div', { clase: 'acciones' },
            Ui.boton('Recargar', async () => Ui.formulario('Recarga', [
                { nombre: 'billeteraDestino', etiqueta: 'Billetera', tipo: 'select', opciones: propias },
                { nombre: 'valor', etiqueta: 'Valor', tipo: 'number', paso: '0.01', minimo: '0.01', requerido: true }],
                'Recargar', async d => { exito(await Api.recargar({ ...d, valor: numero(d.valor) })); await App.refrescar(); })),
            Ui.boton('Retirar', async () => Ui.formulario('Retiro', [
                { nombre: 'billeteraOrigen', etiqueta: 'Billetera', tipo: 'select', opciones: propias },
                { nombre: 'valor', etiqueta: 'Valor', tipo: 'number', paso: '0.01', minimo: '0.01', requerido: true }],
                'Retirar', async d => { exito(await Api.retirar({ ...d, valor: numero(d.valor) })); await App.refrescar(); })),
            Ui.boton('Transferir', async () => Ui.formulario('Transferencia', [
                { nombre: 'billeteraOrigen', etiqueta: 'Desde', tipo: 'select', opciones: propias },
                { nombre: 'billeteraDestino', etiqueta: 'Hacia (código, propia o de otro usuario)', requerido: true, ayuda: 'BIL-000001' },
                { nombre: 'valor', etiqueta: 'Valor', tipo: 'number', paso: '0.01', minimo: '0.01', requerido: true }],
                'Transferir', async d => { exito(await Api.transferir({ ...d, valor: numero(d.valor) })); await App.refrescar(); })),
            Ui.boton('Deshacer última (pila)', async () => {
                const r = await Api.revertirUltima(u);
                Ui.mensaje('Se revirtió ' + r.transaccionRevertida + ' (' + r.id + ')');
                await App.refrescar();
            }, 'secundario'));
        const columnas = COLUMNAS_TRANSACCION.concat([{ titulo: '', valor: t => t.estado === 'EXITOSA' && t.tipo !== 'REVERSION'
            ? Ui.boton('Revertir', async () => {
                const r = await Api.revertir(t.id);
                Ui.mensaje('Reversión ' + r.id + ' registrada');
                await App.refrescar();
            }, 'pequeno secundario') : '' }]);
        const filtroTipo = h('select', {}, ['', 'RECARGA', 'RETIRO', 'TRANSFERENCIA', 'REVERSION'].map(x => h('option', { value: x }, x || 'Todos')));
        const filtroEstado = h('select', {}, ['', 'EXITOSA', 'RECHAZADA', 'REVERTIDA'].map(x => h('option', { value: x }, x || 'Todos')));
        const filtroBilletera = h('select', {}, h('option', { value: '' }, 'Todas'), ...billeteras.map(b => h('option', { value: b.codigo }, b.codigo)));
        const contenedor = h('div');
        // Filtrado solo de presentación sobre el historial que entrega el servidor.
        const pintar = () => contenedor.replaceChildren(tabla(columnas, transacciones.slice().reverse().filter(t =>
            (!filtroTipo.value || t.tipo === filtroTipo.value) && (!filtroEstado.value || t.estado === filtroEstado.value)
            && (!filtroBilletera.value || t.billeteraOrigen === filtroBilletera.value || t.billeteraDestino === filtroBilletera.value))));
        [filtroTipo, filtroEstado, filtroBilletera].forEach(f => f.addEventListener('change', pintar));
        pintar();
        return panel('Transacciones del usuario ' + u, acciones,
            h('p', { clase: 'vacio' }, 'Los rechazos (saldo insuficiente, billetera inactiva) quedan registrados como RECHAZADA.'),
            h('div', { clase: 'filtros' }, h('label', {}, 'Tipo', filtroTipo), h('label', {}, 'Estado', filtroEstado),
                h('label', {}, 'Billetera', filtroBilletera)), contenedor);
    },

    async programadas(u) {
        if (!u) return requiereUsuario();
        const [billeteras, programadas] = await Promise.all([Api.billeteras(u), Api.programadasUsuario(u)]);
        const propias = opcionesBilleteras(billeteras);
        const resultado = h('div');
        const programar = Ui.boton('Programar operación', async () => Ui.formulario('Programar operación', [
            { nombre: 'tipo', etiqueta: 'Tipo', tipo: 'select', opciones: ['RECARGA', 'RETIRO', 'TRANSFERENCIA'] },
            { nombre: 'origen', etiqueta: 'Billetera propia', tipo: 'select', opciones: propias },
            { nombre: 'destino', etiqueta: 'Destino (solo transferencias)', ayuda: 'BIL-000002' },
            { nombre: 'valor', etiqueta: 'Valor', tipo: 'number', paso: '0.01', minimo: '0.01', requerido: true },
            { nombre: 'fechaEjecucion', etiqueta: 'Fecha y hora', tipo: 'datetime-local', requerido: true },
            { nombre: 'periodicidad', etiqueta: 'Periodicidad', tipo: 'select', opciones: ['UNICA', 'SEMANAL', 'MENSUAL'] }],
            'Programar', async d => {
                const cuerpo = { tipo: d.tipo, valor: numero(d.valor), periodicidad: d.periodicidad,
                    fechaEjecucion: d.fechaEjecucion && d.fechaEjecucion.length === 16 ? d.fechaEjecucion + ':00' : d.fechaEjecucion };
                if (d.tipo === 'RECARGA') cuerpo.billeteraDestino = d.origen;
                else cuerpo.billeteraOrigen = d.origen;
                if (d.tipo === 'TRANSFERENCIA') cuerpo.billeteraDestino = d.destino;
                const o = await Api.programar(cuerpo);
                Ui.mensaje('Operación ' + o.id + ' programada');
                await App.refrescar();
            }));
        const procesar = Ui.boton('Ejecutar vencidas ahora', async () => {
            const r = await Api.procesarProgramadas();
            Ui.mensaje(r.ejecutadas + ' ejecutadas, ' + r.fallidas + ' fallidas');
            App.ultimoProcesamiento = r;
            await App.refrescar();
        }, 'secundario');
        const ultimo = App.ultimoProcesamiento;
        if (ultimo) {
            resultado.append(panel('Resultado de la última simulación (' + Formato.fecha(ultimo.procesadoHasta) + ')', tabla([
                { titulo: 'Operación', valor: x => x.operacion }, { titulo: 'Transacción', valor: x => x.transaccion || '—' },
                { titulo: 'Estado', valor: x => Formato.insignia(x.estado) }, { titulo: 'Motivo', valor: x => x.motivo || '—' },
                { titulo: 'Siguiente', valor: x => x.siguienteOperacion || '—' }], ultimo.resultados, 'No había operaciones vencidas.')));
        }
        return h('div', {}, panel('Operaciones programadas del usuario ' + u, h('div', { clase: 'acciones' }, programar, procesar),
            h('p', { clase: 'vacio' }, 'También se ejecutan automáticamente cada minuto (configurable). Se atienden por fecha (cola de prioridad).'),
            tabla([
                { titulo: 'Id', valor: o => o.id }, { titulo: 'Tipo', valor: o => o.tipo },
                { titulo: 'Valor', valor: o => Formato.dinero(o.valor), num: true },
                { titulo: 'Origen', valor: o => o.billeteraOrigen || '—' }, { titulo: 'Destino', valor: o => o.billeteraDestino || '—' },
                { titulo: 'Ejecución', valor: o => Formato.fecha(o.fechaEjecucion) }, { titulo: 'Periodicidad', valor: o => o.periodicidad },
                { titulo: 'Estado', valor: o => Formato.insignia(o.estado) },
                { titulo: '', valor: o => o.estado === 'PENDIENTE' ? Ui.boton('Cancelar', async () => {
                        await Api.cancelarProgramada(o.id); Ui.mensaje('Operación cancelada'); await App.refrescar();
                    }, 'pequeno peligro') : '' }], programadas)), resultado);
    },

    async puntos(u) {
        const [niveles, beneficios] = await Promise.all([Api.niveles(), Api.beneficios()]);
        const nodos = [panel('Niveles', tabla([
            { titulo: 'Nivel', valor: n => Formato.insignia(n.nivel) },
            { titulo: 'Desde', valor: n => n.puntosMinimos, num: true },
            { titulo: 'Hasta', valor: n => n.puntosMaximos === null ? 'sin límite' : n.puntosMaximos, num: true },
            { titulo: 'Ventajas', valor: n => n.ventajas.length ? n.ventajas.join('; ') : 'Por definir (configuración)' }], niveles))];
        if (!u) return h('div', {}, nodos, requiereUsuario());
        const [puntos, canjes] = await Promise.all([Api.puntos(u), Api.canjes(u)]);
        nodos.unshift(h('div', { clase: 'tarjetas' }, tarjeta('Puntos', puntos.puntosAcumulados), tarjeta('Nivel', puntos.nivel),
            tarjeta('Siguiente nivel', puntos.siguienteNivel ? puntos.siguienteNivel + ' (faltan ' + puntos.puntosParaSiguienteNivel + ')' : '—')));
        nodos.push(panel('Catálogo de beneficios', tabla([
            { titulo: 'Código', valor: b => b.codigo }, { titulo: 'Beneficio', valor: b => b.descripcion },
            { titulo: 'Costo', valor: b => b.costo + ' pts', num: true },
            { titulo: '', valor: b => Ui.boton('Canjear', async () => {
                    const c = await Api.canjear(u, b.codigo);
                    Ui.mensaje('Canje ' + c.id + ' realizado'); await App.refrescar();
                }, 'pequeno') }], beneficios, 'El catálogo no está definido (fintech.beneficios.catalogo).')));
        nodos.push(panel('Beneficios canjeados', tabla([
            { titulo: 'Id', valor: c => c.id }, { titulo: 'Beneficio', valor: c => c.beneficio },
            { titulo: 'Puntos', valor: c => c.puntosDescontados, num: true }, { titulo: 'Fecha', valor: c => Formato.fecha(c.fecha) },
            { titulo: 'Estado', valor: c => Formato.insignia(c.estado) }], canjes)));
        return h('div', {}, nodos);
    },

    async notificaciones(u) {
        const pendientes = await Api.pendientes();
        const despachar = Ui.boton('Despachar pendientes (FIFO)', async () => {
            const r = await Api.despachar();
            Ui.mensaje(r.length + ' notificaciones despachadas'); await App.refrescar();
        });
        const columnas = [
            { titulo: 'Id', valor: n => n.id }, { titulo: 'Usuario', valor: n => n.identificacionUsuario },
            { titulo: 'Tipo', valor: n => n.tipo }, { titulo: 'Mensaje', valor: n => n.mensaje },
            { titulo: 'Fecha', valor: n => Formato.fecha(n.fecha) }, { titulo: 'Estado', valor: n => Formato.insignia(n.estado) }];
        const nodos = [panel('Cola de pendientes (' + pendientes.length + ')', h('div', { clase: 'acciones' }, despachar),
            tabla(columnas, pendientes))];
        if (u) nodos.unshift(panel('Recientes del usuario ' + u, tabla(columnas, await Api.notificaciones(u))));
        return h('div', {}, nodos);
    },

    async auditoria(u) {
        const riesgo = h('select', {}, RIESGOS.map(r => h('option', { value: r }, r || 'Todos')));
        const soloUsuario = h('input', { type: 'checkbox', checked: Boolean(u) });
        const contenedor = h('div');
        const cargar = async () => {
            const eventos = soloUsuario.checked && u ? await Api.auditoriaUsuario(u, riesgo.value) : await Api.auditoria(riesgo.value);
            contenedor.replaceChildren(tabla([
                { titulo: 'Id', valor: e => e.id }, { titulo: 'Usuario', valor: e => e.identificacionUsuario },
                { titulo: 'Transacción', valor: e => e.transaccion || '—' }, { titulo: 'Patrón', valor: e => e.patron },
                { titulo: 'Riesgo', valor: e => Formato.insignia(e.nivelRiesgo) }, { titulo: 'Descripción', valor: e => e.descripcion },
                { titulo: 'Fecha', valor: e => Formato.fecha(e.fecha) }], eventos, 'Sin eventos de actividad inusual.'));
        };
        await cargar();
        return panel('Auditoría de actividad inusual', h('div', { clase: 'filtros' },
            h('label', {}, 'Riesgo', riesgo), h('label', {}, 'Solo usuario seleccionado', soloUsuario),
            Ui.boton('Aplicar', cargar, 'secundario')), contenedor);
    },

    async analitica(u) {
        const [billeteras, transferencias, categorias, frecuencia, mayores, grafo, frecuentes] = await Promise.all([
            Api.reporte('billeteras-mas-usadas', { cantidad: 5 }), Api.reporte('usuarios-mas-transferencias', { cantidad: 5 }),
            Api.reporte('categorias-activas'), Api.reporte('frecuencia-por-tipo'),
            Api.reporte('transacciones-mayor-valor', { cantidad: 5 }), Api.grafo('MONTO'), Api.rutasFrecuentes(5, 'MONTO')]);
        const conteo = (titulo, lista) => panel(titulo, tabla([
            { titulo: 'Clave', valor: c => c.clave }, { titulo: 'Cantidad', valor: c => c.cantidad, num: true },
            { titulo: '', valor: c => h('div', { clase: 'barra', style: 'width:' + Math.max(2, 120 * c.cantidad / Math.max(1, lista[0] ? lista[0].cantidad : 1)) + 'px' }) }], lista));
        const desde = h('input', { type: 'datetime-local' });
        const hasta = h('input', { type: 'datetime-local' });
        const periodo = h('div');
        const seg = v => v && v.length === 16 ? v + ':00' : v;
        const origen = h('input', { placeholder: 'usuario origen', value: u || '' });
        const destino = h('input', { placeholder: 'usuario destino' });
        const criterio = h('select', {}, ['CANTIDAD', 'MONTO'].map(c => h('option', { value: c }, c)));
        const longitud = h('input', { type: 'number', min: 1, max: 10, value: 4 });
        const resultadoGrafo = h('div');
        return h('div', {},
            h('div', { clase: 'tarjetas' }, tarjeta('Usuarios en el grafo', grafo.vertices.length),
                tarjeta('Relaciones', grafo.aristas.length), tarjeta('¿Hay ciclos?', grafo.tieneCiclo ? 'Sí' : 'No')),
            panel('Monto movilizado y usuario más activo', h('div', { clase: 'filtros' },
                h('label', {}, 'Desde', desde), h('label', {}, 'Hasta', hasta),
                Ui.boton('Calcular', async () => {
                    const [m, activo] = await Promise.all([
                        Api.reporte('monto-movilizado', { desde: seg(desde.value), hasta: seg(hasta.value) }),
                        Api.reporte('usuario-mas-activo', { desde: seg(desde.value), hasta: seg(hasta.value) }).catch(e => ({ clave: e.codigo, cantidad: 0 }))]);
                    periodo.replaceChildren(h('div', { clase: 'tarjetas' }, tarjeta('Monto movilizado', Formato.dinero(m.total)),
                        tarjeta('Transacciones', m.transacciones), tarjeta('Más activo', activo.clave + ' (' + activo.cantidad + ')')));
                }, 'secundario')), periodo),
            conteo('Billeteras más usadas', billeteras), conteo('Usuarios con más transferencias', transferencias),
            conteo('Categorías (tipos de billetera) más activas', categorias), conteo('Frecuencia por tipo', frecuencia),
            panel('Transacciones de mayor valor', tabla(COLUMNAS_TRANSACCION, mayores)),
            panel('Relaciones más frecuentes (monto)', tabla([
                { titulo: 'Origen', valor: a => a.origen }, { titulo: 'Destino', valor: a => a.destino },
                { titulo: 'Monto acumulado', valor: a => Formato.dinero(a.peso), num: true }], frecuentes)),
            panel('Rutas y ciclos entre usuarios', h('div', { clase: 'filtros' },
                h('label', {}, 'Origen', origen), h('label', {}, 'Destino', destino), h('label', {}, 'Peso', criterio),
                Ui.boton('Buscar ruta', async () => {
                    const r = await Api.rutas(origen.value, destino.value, criterio.value);
                    resultadoGrafo.replaceChildren(
                        h('p', {}, 'Menos saltos: ', r.menosSaltos.length ? h('span', { clase: 'ruta' }, r.menosSaltos.join(' → ')) : 'sin ruta'),
                        h('p', {}, 'Menor peso (' + r.criterio + '): ', r.menorPeso.length
                            ? h('span', { clase: 'ruta' }, r.menorPeso.join(' → ') + '  [' + r.pesoMenor + ']') : 'sin ruta'));
                }, 'secundario'),
                h('label', {}, 'Longitud máx. ciclo', longitud),
                Ui.boton('Ciclos del origen', async () => {
                    const c = await Api.ciclos(origen.value, longitud.value);
                    resultadoGrafo.replaceChildren(c.ciclos.length
                        ? h('div', {}, c.ciclos.map(x => h('div', {}, h('span', { clase: 'ruta' }, x.join(' → ')))))
                        : h('p', { clase: 'vacio' }, 'Sin ciclos que incluyan a ' + c.usuario + '.'));
                }, 'secundario')), resultadoGrafo));
    },

    async benchmark() {
        const comparacion = h('select', {}, ['BUSQUEDA', 'CONSULTA_ORDENADA', 'PRIORIDAD', 'RECORRIDO_GRAFO'].map(c => h('option', { value: c }, c)));
        const elementos = h('input', { type: 'number', min: 10, max: 5000, value: 1000 });
        const repeticiones = h('input', { type: 'number', min: 1, max: 20, value: 5 });
        const resultado = h('div');
        return panel('Benchmark de estructuras', h('div', { clase: 'filtros' },
            h('label', {}, 'Comparación', comparacion), h('label', {}, 'Elementos (10–5000)', elementos),
            h('label', {}, 'Repeticiones (1–20)', repeticiones),
            Ui.boton('Medir', async () => {
                const r = await Api.benchmark(comparacion.value, elementos.value, repeticiones.value);
                resultado.replaceChildren(tabla([
                    { titulo: 'Operación', valor: m => m.operacion }, { titulo: 'Estructura', valor: m => m.estructura },
                    { titulo: 'Promedio (µs)', valor: m => (m.promedioNanos / 1000).toFixed(1), num: true },
                    { titulo: 'Mínimo (µs)', valor: m => (m.minimoNanos / 1000).toFixed(1), num: true },
                    { titulo: 'Máximo (µs)', valor: m => (m.maximoNanos / 1000).toFixed(1), num: true },
                    { titulo: 'Resultado', valor: m => m.resultado }], r.mediciones), h('p', { clase: 'vacio' }, r.limitaciones));
            })), resultado);
    }
};
