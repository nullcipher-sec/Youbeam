import './card-video.css'

type CardVideoVariant = 'grid' | 'list'

type CardVideoProps = {
    videoId: string
    title: string
    creator: string
    views: string
    publishedAt: string
    duration: string
    thumbnail: string
    avatar?: string
    badge?: string
    description?: string
    verified?: boolean
    variant?: CardVideoVariant
    onPlay?: (videoId: string) => void
}

function MoreIcon() {
    return (
        <svg viewBox="0 0 24 24" aria-hidden="true">
            <circle cx="5" cy="12" r="1.5" />
            <circle cx="12" cy="12" r="1.5" />
            <circle cx="19" cy="12" r="1.5" />
        </svg>
    )
}

function VerifiedIcon() {
    return (
        <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="m12 3 2.1 1.7 2.7-.2 1.1 2.5 2.3 1.3-.7 2.6.7 2.6-2.3 1.3-1.1 2.5-2.7-.2L12 21l-2.1-1.7-2.7.2-1.1-2.5-2.3-1.3.7-2.6-.7-2.6 2.3-1.3 1.1-2.5 2.7.2L12 3Z" />
            <path d="m8.5 12 2.2 2.2 4.8-4.8" />
        </svg>
    )
}

function CardVideo({
    title,
    creator,
    views,
    publishedAt,
    duration,
    thumbnail,
    avatar,
    badge,
    description,
    verified = false,
    variant = 'grid',
    videoId,
    onPlay,
}: CardVideoProps) {
    return (
        <article className={`video-card video-card--${variant}`} onClick={() => onPlay?.(videoId)} tabIndex={onPlay ? 0 : undefined} onKeyDown={(event) => {
            if (onPlay && (event.key === 'Enter' || event.key === ' ')) {
                event.preventDefault()
                onPlay(videoId)
            }
        }}>
            <div className="video-card-media">
                <img src={thumbnail} alt="" />
                {badge && <span className="video-card-badge">{badge}</span>}
                <span className="video-card-duration">{duration}</span>
            </div>

            <div className="video-card-content">
                <div className="video-card-heading">
                    <h2>{title}</h2>
                    {variant === 'list' && (
                        <button className="video-card-more" type="button" aria-label={`Más opciones para ${title}`} onClick={(event) => event.stopPropagation()}>
                            <MoreIcon />
                        </button>
                    )}
                </div>

                <div className="video-card-creator">
                    {avatar ? (
                        <img src={avatar} alt="" />
                    ) : (
                        <span className="video-card-avatar-placeholder" aria-hidden="true">
                            {creator.charAt(0)}
                        </span>
                    )}
                    <span>{creator}</span>
                    {verified && (
                        <span className="video-card-verified" title="Creador verificado">
                            <VerifiedIcon />
                        </span>
                    )}
                </div>

                <p className="video-card-meta">
                    {views} vistas <span aria-hidden="true">•</span> hace {publishedAt}
                </p>

                {description && <p className="video-card-description">{description}</p>}
            </div>
        </article>
    )
}

export type { CardVideoProps, CardVideoVariant }
export default CardVideo
