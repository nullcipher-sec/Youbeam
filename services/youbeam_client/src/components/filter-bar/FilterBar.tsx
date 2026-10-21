import { useState, useRef, useEffect } from 'react'
import './filter-bar.css'

const categories = ['Todos', 'Tendencias', 'Tutoriales', 'Música', 'Gaming', 'Diseño UI/UX', 'Fotografía 4K']

type FilterBarProps = {
    onCategoryChange?: (category: string) => void
    onSortChange?: (sort: string) => void
    onViewChange?: (view: 'grid' | 'list') => void
    view?: 'grid' | 'list'
}

function SlidersIcon() {
    return (
        <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M4 6h5M15 6h5M4 12h9M19 12h1M4 18h2M12 18h8" />
            <circle cx="12" cy="6" r="2" />
            <circle cx="16" cy="12" r="2" />
            <circle cx="9" cy="18" r="2" />
        </svg>
    )
}

function GridIcon() {
    return (
        <svg viewBox="0 0 24 24" aria-hidden="true">
            <rect x="4" y="4" width="6" height="6" rx="1" />
            <rect x="14" y="4" width="6" height="6" rx="1" />
            <rect x="4" y="14" width="6" height="6" rx="1" />
            <rect x="14" y="14" width="6" height="6" rx="1" />
        </svg>
    )
}

function FilterBar({ onCategoryChange, onSortChange, onViewChange, view: controlledView }: FilterBarProps) {
    const [activeCategory, setActiveCategory] = useState('Todos')
    const [sort, setSort] = useState('Más recientes')
    const [internalView, setInternalView] = useState<'grid' | 'list'>('grid')
    const view = controlledView ?? internalView

    const [isSortMenuOpen, setIsSortMenuOpen] = useState(false)
    const sortMenuRef = useRef<HTMLDivElement>(null)

    useEffect(() => {
        function handleClickOutside(event: MouseEvent) {
            if (sortMenuRef.current && !sortMenuRef.current.contains(event.target as Node)) {
                setIsSortMenuOpen(false)
            }
        }

        document.addEventListener('mousedown', handleClickOutside)
        return () => {
            document.removeEventListener('mousedown', handleClickOutside)
        }
    }, [])

    function handleCategoryChange(category: string) {
        setActiveCategory(category)
        onCategoryChange?.(category)
    }

    function handleSortSelect(option: string) {
        setSort(option)
        onSortChange?.(option)
        setIsSortMenuOpen(false)
    }

    function handleViewChange() {
        const nextView = view === 'grid' ? 'list' : 'grid'
        setInternalView(nextView)
        onViewChange?.(nextView)
    }

    return (
        <nav className="filter-bar" aria-label="Filtros de contenido">
            <div className="filter-bar-categories">
                {categories.map((category) => (
                    <button
                        className={`filter-category ${activeCategory === category ? 'is-active' : ''}`}
                        key={category}
                        type="button"
                        aria-pressed={activeCategory === category}
                        onClick={() => handleCategoryChange(category)}
                    >
                        {category}
                    </button>
                ))}
            </div>

            <div className="filter-bar-actions">
                <div className="filter-sort-container" ref={sortMenuRef}>
                    <button
                        className="filter-sort"
                        type="button"
                        onClick={() => setIsSortMenuOpen(!isSortMenuOpen)}
                        aria-expanded={isSortMenuOpen}
                    >
                        <SlidersIcon />
                        <span>Filtros: {sort}</span>
                    </button>

                    {isSortMenuOpen && (
                        <div className="sort-dropdown">
                            <button
                                className="sort-dropdown-item"
                                onClick={() => handleSortSelect('Más recientes')}
                            >
                                Más recientes
                            </button>
                            <button
                                className="sort-dropdown-item"
                                onClick={() => handleSortSelect('Más populares')}
                            >
                                Más populares
                            </button>
                        </div>
                    )}
                </div>

                <button
                    className={`filter-view ${view === 'grid' ? 'is-active' : ''}`}
                    type="button"
                    aria-label={view === 'grid' ? 'Cambiar a vista de lista' : 'Cambiar a vista de cuadrícula'}
                    aria-pressed={view === 'grid'}
                    onClick={handleViewChange}
                >
                    <GridIcon />
                </button>
            </div>
        </nav>
    )
}

export default FilterBar