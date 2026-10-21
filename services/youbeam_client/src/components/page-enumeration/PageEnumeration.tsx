import './page-enumeration.css'

type PageItem = number | 'ellipsis'

type PageEnumerationProps = {
    totalItems: number
    itemsPerPage: number
    currentPage: number
    onPageChange?: (page: number) => void
}

function getPageItems(currentPage: number, totalPages: number): PageItem[] {
    if (totalPages <= 5) {
        return Array.from({ length: totalPages }, (_, index) => index + 1)
    }

    if (currentPage <= 3) {
        return [1, 2, 3, 'ellipsis', totalPages]
    }

    if (currentPage >= totalPages - 2) {
        return [1, 'ellipsis', totalPages - 2, totalPages - 1, totalPages]
    }

    return [1, 'ellipsis', currentPage, 'ellipsis', totalPages]
}

function PageEnumeration({
                             totalItems,
                             itemsPerPage,
                             currentPage,
                             onPageChange,
                         }: PageEnumerationProps) {
    const totalPages = Math.max(1, Math.ceil(totalItems / itemsPerPage))
    const safeCurrentPage = Math.min(Math.max(currentPage, 1), totalPages)
    const pageItems = getPageItems(safeCurrentPage, totalPages)

    function changePage(page: number) {
        if (page !== safeCurrentPage) {
            onPageChange?.(page)
        }
    }

    return (
        <nav className="page-enumeration" aria-label="Paginación de videos">
            <div className="page-enumeration-controls">
                <button
                    className="page-enumeration-navigation"
                    type="button"
                    disabled={safeCurrentPage === 1}
                    onClick={() => changePage(safeCurrentPage - 1)}
                >
                    Anterior
                </button>

                <div className="page-enumeration-pages">
                    {pageItems.map((item, index) =>
                        item === 'ellipsis' ? (
                            <span className="page-enumeration-ellipsis" key={`ellipsis-${index}`}>
                                ...
                            </span>
                        ) : (
                            <button
                                className={`page-enumeration-page ${item === safeCurrentPage ? 'is-active' : ''}`}
                                key={item}
                                type="button"
                                aria-label={`Ir a la página ${item}`}
                                aria-current={item === safeCurrentPage ? 'page' : undefined}
                                onClick={() => changePage(item)}
                            >
                                {item}
                            </button>
                        ),
                    )}
                </div>

                <button
                    className="page-enumeration-navigation"
                    type="button"
                    disabled={safeCurrentPage === totalPages}
                    onClick={() => changePage(safeCurrentPage + 1)}
                >
                    Siguiente
                </button>
            </div>
        </nav>
    )
}

export default PageEnumeration