// Cliente de la API REST. Toda regla financiera vive en el servidor: aquí solo se envía y se muestra.
'use strict';

class ErrorApi extends Error {
    constructor(estado, codigo, mensaje) {
        super(mensaje);
        this.estado = estado;
        this.codigo = codigo;
    }
}

async function api(metodo, ruta, cuerpo) {
    const opciones = { method: metodo, headers: { Accept: 'application/json' } };
    if (cuerpo !== undefined) {
        opciones.headers['Content-Type'] = 'application/json';
        opciones.body = JSON.stringify(cuerpo);
    }
    let respuesta;
    try {
        respuesta = await fetch(ruta, opciones);
    } catch (e) {
        throw new ErrorApi(0, 'SIN_CONEXION', 'No se pudo contactar el servidor');
    }
    if (respuesta.status === 204) {
        return null;
    }
    const texto = await respuesta.text();
    let datos = null;
    try {
        datos = texto ? JSON.parse(texto) : null;
    } catch (e) {
        datos = null;
    }
    if (!respuesta.ok) {
        const codigo = datos && datos.error ? datos.error : 'HTTP_' + respuesta.status;
        const mensaje = datos && datos.mensaje ? datos.mensaje : 'Error ' + respuesta.status;
        throw new ErrorApi(respuesta.status, codigo, mensaje);
    }
    return datos;
}

function consulta(parametros) {
    const p = new URLSearchParams();
    Object.entries(parametros).forEach(([k, v]) => {
        if (v !== undefined && v !== null && v !== '') p.append(k, v);
    });
    const s = p.toString();
    return s ? '?' + s : '';
}

const Api = {
    usuarios: () => api('GET', '/api/usuarios'),
    usuario: id => api('GET', '/api/usuarios/' + encodeURIComponent(id)),
    registrarUsuario: d => api('POST', '/api/usuarios', d),
    actualizarUsuario: (id, d) => api('PUT', '/api/usuarios/' + encodeURIComponent(id), d),
    eliminarUsuario: id => api('DELETE', '/api/usuarios/' + encodeURIComponent(id)),
    puntos: id => api('GET', '/api/usuarios/' + encodeURIComponent(id) + '/puntos'),
    porPuntos: (min, max) => api('GET', '/api/usuarios/por-puntos' + consulta({ min, max })),
    porNivel: n => api('GET', '/api/usuarios/por-nivel/' + n),

    saldoTotal: id => api('GET', '/api/usuarios/' + encodeURIComponent(id) + '/saldo-total'),
    billeteras: id => api('GET', '/api/usuarios/' + encodeURIComponent(id) + '/billeteras'),
    crearBilletera: (id, d) => api('POST', '/api/usuarios/' + encodeURIComponent(id) + '/billeteras', d),
    actualizarBilletera: (c, d) => api('PUT', '/api/billeteras/' + c, d),
    desactivarBilletera: c => api('DELETE', '/api/billeteras/' + c),
    activarBilletera: c => api('POST', '/api/billeteras/' + c + '/activacion'),

    recargar: d => api('POST', '/api/transacciones/recargas', d),
    retirar: d => api('POST', '/api/transacciones/retiros', d),
    transferir: d => api('POST', '/api/transacciones/transferencias', d),
    transaccionesUsuario: id => api('GET', '/api/usuarios/' + encodeURIComponent(id) + '/transacciones'),
    transaccionesBilletera: c => api('GET', '/api/billeteras/' + c + '/transacciones'),
    revertir: id => api('POST', '/api/transacciones/' + id + '/reversion'),
    revertirUltima: id => api('POST', '/api/usuarios/' + encodeURIComponent(id) + '/reversion/ultima'),

    programadas: estado => api('GET', '/api/operaciones-programadas' + consulta({ estado })),
    programadasUsuario: id => api('GET', '/api/usuarios/' + encodeURIComponent(id) + '/operaciones-programadas'),
    programar: d => api('POST', '/api/operaciones-programadas', d),
    cancelarProgramada: id => api('DELETE', '/api/operaciones-programadas/' + id),
    procesarProgramadas: () => api('POST', '/api/operaciones-programadas/procesar'),

    niveles: () => api('GET', '/api/niveles'),
    beneficios: () => api('GET', '/api/beneficios'),
    canjes: id => api('GET', '/api/usuarios/' + encodeURIComponent(id) + '/canjes'),
    canjear: (id, beneficio) => api('POST', '/api/usuarios/' + encodeURIComponent(id) + '/canjes', { beneficio }),

    notificaciones: id => api('GET', '/api/usuarios/' + encodeURIComponent(id) + '/notificaciones'),
    pendientes: () => api('GET', '/api/notificaciones/pendientes'),
    despachar: () => api('POST', '/api/notificaciones/despachar'),

    auditoria: riesgo => api('GET', '/api/auditoria' + consulta({ riesgo })),
    auditoriaUsuario: (id, riesgo) => api('GET', '/api/usuarios/' + encodeURIComponent(id) + '/auditoria' + consulta({ riesgo })),

    reporte: (nombre, p) => api('GET', '/api/reportes/' + nombre + consulta(p || {})),
    grafo: criterio => api('GET', '/api/grafo/transferencias' + consulta({ criterio })),
    rutas: (origen, destino, criterio) => api('GET', '/api/grafo/rutas' + consulta({ origen, destino, criterio })),
    rutasFrecuentes: (cantidad, criterio) => api('GET', '/api/grafo/rutas-frecuentes' + consulta({ cantidad, criterio })),
    ciclos: (usuario, longitudMaxima) => api('GET', '/api/grafo/ciclos' + consulta({ usuario, longitudMaxima })),
    benchmark: (comparacion, elementos, repeticiones) =>
        api('GET', '/api/benchmark' + consulta({ comparacion, elementos, repeticiones }))
};
